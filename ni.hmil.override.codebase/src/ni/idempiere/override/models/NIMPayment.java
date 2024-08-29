package ni.idempiere.override.models;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.PeriodClosedException;
import org.adempiere.util.PaymentUtil;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPaySelectionLine;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.MPeriod;
import org.compiere.model.MSysConfig;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.Obscure;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

public class NIMPayment extends MPayment 
{ 
	CLogger log=CLogger.getCLogger(NIMPayment.class);
	
	/**	Process Message 			*/
	private String		m_processMsg;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8248599698974482671L;

	public NIMPayment(Properties ctx, int C_Payment_ID, String trxName) {
		super(ctx, C_Payment_ID, trxName);	 
	}
	
	public NIMPayment(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (isComplete() && 
				! is_ValueChanged(COLUMNNAME_Processed) &&
	            (   is_ValueChanged(COLUMNNAME_C_BankAccount_ID)
	             || is_ValueChanged(COLUMNNAME_C_BPartner_ID)
	             || is_ValueChanged(COLUMNNAME_C_Charge_ID)
	             || is_ValueChanged(COLUMNNAME_C_Currency_ID)
	             || is_ValueChanged(COLUMNNAME_C_DocType_ID)
	             || is_ValueChanged(COLUMNNAME_DateAcct)
	             || is_ValueChanged(COLUMNNAME_DateTrx)
	             || is_ValueChanged(COLUMNNAME_DiscountAmt)
	             || is_ValueChanged(COLUMNNAME_PayAmt)
	             || is_ValueChanged(COLUMNNAME_WriteOffAmt))) {
				log.saveError("PaymentAlreadyProcessed", Msg.translate(getCtx(), "C_Payment_ID"));
				return false;
			}
			// @Trifon - CashPayments
			//if ( getTenderType().equals("X") ) {
			if ( isCashbookTrx()) {
				// Cash Book Is mandatory
				if ( getC_CashBook_ID() <= 0 ) {
					log.saveError("Error", Msg.parseTranslation(getCtx(), "@Mandatory@: @C_CashBook_ID@"));
					return false;
				}
			} else {
				// Bank Account Is mandatory
				if ( getC_BankAccount_ID() <= 0 ) {
					log.saveError("Error", Msg.parseTranslation(getCtx(), "@Mandatory@: @C_BankAccount_ID@"));
					return false;
				}
			}
			// end @Trifon - CashPayments
			
			//	We have a charge
			if (getC_Charge_ID() != 0) 
			{
				if (newRecord || is_ValueChanged("C_Charge_ID"))
				{
					setC_Order_ID(0);
					setC_Invoice_ID(0);
					setWriteOffAmt(Env.ZERO);
					setDiscountAmt(Env.ZERO);
					setIsOverUnderPayment(false);
					setOverUnderAmt(Env.ZERO);
					setIsPrepayment(false);
				}
			}
			//	We need a BPartner
			else if (getC_BPartner_ID() == 0 && !isCashTrx())
			{
				if (getC_Invoice_ID() != 0)
					;
				else if (getC_Order_ID() != 0)
					;
				else
				{
					log.saveError("Error", Msg.parseTranslation(getCtx(), "@NotFound@: @C_BPartner_ID@"));
					return false;
				}
			}
			//	Prepayment: No charge and order or project (not as acct dimension)
			/*if (newRecord 
				|| is_ValueChanged("C_Charge_ID") || is_ValueChanged("C_Invoice_ID")
				|| is_ValueChanged("C_Order_ID") || is_ValueChanged("C_Project_ID"))
				setIsPrepayment (getC_Charge_ID() == 0 
					&& getC_BPartner_ID() != 0
					&& (getC_Order_ID() != 0 
						|| (getC_Project_ID() != 0 && getC_Invoice_ID() == 0)));
			*/
			/*if (newRecord
                    || is_ValueChanged("C_Charge_ID") || is_ValueChanged("C_Invoice_ID")
                   || is_ValueChanged("C_Order_ID") || is_ValueChanged("C_Project_ID"))
                         setIsPrepayment (getC_Charge_ID() == 0
                          && getC_BPartner_ID() != 0
                               && (getC_Order_ID() == 0
                               || (getC_Project_ID() == 0 && getC_Invoice_ID() == 0)));*/
			
			/*if (isPrepayment())
			{
				if (newRecord 
					|| is_ValueChanged("C_Order_ID") || is_ValueChanged("C_Project_ID"))
				{
					setWriteOffAmt(Env.ZERO);
					setDiscountAmt(Env.ZERO);
					setIsOverUnderPayment(false);
					setOverUnderAmt(Env.ZERO);
				}
			}*/
			
			//	Document Type/Receipt
			if (getC_DocType_ID() == 0)
				super.setC_DocType_ID(isReceipt());
			else
			{
				MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
				setIsReceipt(dt.isSOTrx());
			}
			setDocumentNo();
			//
			if (getDateAcct() == null)
				setDateAcct(getDateTrx());
			//
			if (!isOverUnderPayment())
				setOverUnderAmt(Env.ZERO);
			
			//	Organization
			if ((newRecord || is_ValueChanged("C_BankAccount_ID"))
				&& getC_Charge_ID() == 0)	//	allow different org for charge
			{
				MBankAccount ba = MBankAccount.get(getCtx(), getC_BankAccount_ID());
				if (ba.getAD_Org_ID() != 0)
					setAD_Org_ID(ba.getAD_Org_ID());
			}
			
			// [ adempiere-Bugs-1885417 ] Validate BP on Payment Prepare or BeforeSave
			// there is bp and (invoice or order)
			if (getC_BPartner_ID() != 0 && (getC_Invoice_ID() != 0 || getC_Order_ID() != 0)) {
				if (getC_Invoice_ID() != 0) {
					MInvoice inv = new MInvoice(getCtx(), getC_Invoice_ID(), get_TrxName());
					if (inv.getC_BPartner_ID() != getC_BPartner_ID()) {
						log.saveError("Error", Msg.parseTranslation(getCtx(), "BP different from BP Invoice"));
						return false;
					}
				}
				if (getC_Order_ID() != 0) {
					MOrder ord = new MOrder(getCtx(), getC_Order_ID(), get_TrxName());
					if (ord.getC_BPartner_ID() != getC_BPartner_ID()) {
						log.saveError("Error", Msg.parseTranslation(getCtx(), "BP different from BP Order"));
						return false;
					}
				}
			}
			
			if (isProcessed())
			{
				if (getCreditCardNumber() != null)
				{
					String encrpytedCCNo = PaymentUtil.encrpytCreditCard(getCreditCardNumber());
					if (!encrpytedCCNo.equals(getCreditCardNumber()))
						setCreditCardNumber(encrpytedCCNo);
				}
				
				if (getCreditCardVV() != null)
				{
					String encrpytedCvv = PaymentUtil.encrpytCvv(getCreditCardVV());
					if (!encrpytedCvv.equals(getCreditCardVV()))
						setCreditCardVV(encrpytedCvv);
				}
			}
			log.severe("Log desde NINSSMPayment...");
			return true;
	}
	
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		 
		return super.afterSave(newRecord, success);
	}
	
	/**
	 *  Set DocumentNo to Payment info.
	 * 	If there is a R_PnRef that is set automatically 
	 */
	protected void setDocumentNo()
	{
		//	Cash Transfer
		if ("X".equals(getTenderType()))
			return;
		//	Current Document No
		String documentNo = getDocumentNo();
		//	Existing reversal
		if (documentNo != null 
			&& documentNo.indexOf(REVERSE_INDICATOR) >= 0)
			return;
		
		//	If external number exists - enforce it 
		if (getR_PnRef() != null && getR_PnRef().length() > 0)
		{
			if (!getR_PnRef().equals(documentNo))
				setDocumentNo(getR_PnRef());
			return;
		}
		
		documentNo = "";
		// globalqss - read configuration to assign credit card or check number number for Payments
		//	Credit Card
		if (TENDERTYPE_CreditCard.equals(getTenderType()))
		{
			if (MSysConfig.getBooleanValue(MSysConfig.PAYMENT_OVERWRITE_DOCUMENTNO_WITH_CREDIT_CARD, true, getAD_Client_ID())) {
				documentNo = getCreditCardType()
					+ " " + Obscure.obscure(getCreditCardNumber())
					+ " " + getCreditCardExpMM() 
					+ "/" + getCreditCardExpYY();
			}
		}
		//	Own Check No
		else if (TENDERTYPE_Check.equals(getTenderType())
			&& !isReceipt()
			&& getCheckNo() != null && getCheckNo().length() > 0)
		{
			if (MSysConfig.getBooleanValue(MSysConfig.PAYMENT_OVERWRITE_DOCUMENTNO_WITH_CHECK_ON_PAYMENT, true, getAD_Client_ID())) {
				documentNo = getCheckNo();
			}
		}
		//	Customer Check: Routing: Account #Check 
		else if (TENDERTYPE_Check.equals(getTenderType())
			&& isReceipt())
		{
			if (MSysConfig.getBooleanValue(MSysConfig.PAYMENT_OVERWRITE_DOCUMENTNO_WITH_CHECK_ON_RECEIPT, true, getAD_Client_ID())) {
				if (getRoutingNo() != null)
					documentNo = getRoutingNo() + ": ";
				if (getAccountNo() != null)
					documentNo += getAccountNo();
				if (getCheckNo() != null)
				{
					if (documentNo.length() > 0)
						documentNo += " ";
					documentNo += "#" + getCheckNo();
				}
			}
		}

		//	Set Document No
		documentNo = documentNo.trim();
		if (documentNo.length() > 0)
			setDocumentNo(documentNo);
	}	//	setDocumentNo
 
	@Override
	public boolean voidIt() 
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());		
		
		if (DOCSTATUS_Closed.equals(getDocStatus())
			|| DOCSTATUS_Reversed.equals(getDocStatus())
			|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			m_processMsg = "Document Closed: " + getDocStatus();
			setDocAction(DOCACTION_None);
			return false;
		}
		//	If on Bank Statement, don't void it - reverse it
		if (getC_BankStatementLine_ID() > 0)
			return reverseCorrectIt();
		
		//	Not Processed
		if (DOCSTATUS_Drafted.equals(getDocStatus())
			|| DOCSTATUS_Invalid.equals(getDocStatus())
			|| DOCSTATUS_InProgress.equals(getDocStatus())
			|| DOCSTATUS_Approved.equals(getDocStatus())
			|| DOCSTATUS_NotApproved.equals(getDocStatus()) )
		{
			// Before Void
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
			if (m_processMsg != null)
				return false;
			
			if (!voidOnlinePayment())
				return false;
			
			addDescription(Msg.getMsg(getCtx(), "Voided") + " (" + getPayAmt() + ")");
			setPayAmt(Env.ZERO);
			setDiscountAmt(Env.ZERO);
			setWriteOffAmt(Env.ZERO);
			setOverUnderAmt(Env.ZERO);
			setIsAllocated(false);
			//	Unlink & De-Allocate
			deAllocate(false);
		}
		else
		{
			boolean accrual = false;
			try 
			{
				MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
			}
			catch (PeriodClosedException e) 
			{
				accrual = true;
			}
			
			if (accrual)
				return reverseAccrualIt();
			else
				return reverseCorrectIt();
		}
		
		//
		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	} 
	
	protected void deAllocate(boolean accrual) {
		// TODO Auto-generated method stub
		// if (getC_Order_ID() != 0) setC_Order_ID(0); // IDEMPIERE-1764
		//	if (getC_Invoice_ID() == 0)
		//		return;
			//	De-Allocate all 
		
			MAllocationHdr[] allocations = MAllocationHdr.getOfPayment(getCtx(), 
				getC_Payment_ID(), get_TrxName());
		
			if (log.isLoggable(Level.FINE)) log.fine("#" + allocations.length);
			
			for (int i = 0; i < allocations.length; i++)
			{
				allocations[i].set_TrxName(get_TrxName());
				if (DOCSTATUS_Reversed.equals(allocations[i].getDocStatus())
					|| DOCSTATUS_Voided.equals(allocations[i].getDocStatus()))
				{
					continue;
				}
				
				if (accrual) 
				{
					allocations[i].setDocAction(DocAction.ACTION_Reverse_Accrual);
					if (!allocations[i].processIt(DocAction.ACTION_Reverse_Accrual))
						throw new AdempiereException(allocations[i].getProcessMsg());
				}
				else
				{
					allocations[i].setDocAction(DocAction.ACTION_Reverse_Correct);
					if (!allocations[i].processIt(DocAction.ACTION_Reverse_Correct))
						throw new AdempiereException(allocations[i].getProcessMsg());
				}
				allocations[i].saveEx();
			}
			
			// 	Unlink (in case allocation did not get it)
			if (getC_Invoice_ID() != 0)
		
			{
				//	Invoice					
			
				String sql = "UPDATE C_Invoice "
					+ "SET C_Payment_ID = NULL, IsPaid='N' "
					+ "WHERE C_Invoice_ID=" + getC_Invoice_ID()
					+ " AND C_Payment_ID=" + getC_Payment_ID();
				
				int no = DB.executeUpdate(sql, get_TrxName());
				
		
				if (no != 0)
					if (log.isLoggable(Level.FINE)) log.fine("Unlink Invoice #" + no);
				
				//	Order
				
				sql = "UPDATE C_Order o "
					+ "SET C_Payment_ID = NULL "
					+ "WHERE EXISTS (SELECT * FROM C_Invoice i "
						+ "WHERE o.C_Order_ID=i.C_Order_ID AND i.C_Invoice_ID=" + getC_Invoice_ID() + ")"
					+ " AND C_Payment_ID=" + getC_Payment_ID();
				
				no = DB.executeUpdate(sql, get_TrxName());
				
				if (no != 0)
					if (log.isLoggable(Level.FINE)) log.fine("Unlink Order #" + no);
			}
			
			//
			
			setC_Invoice_ID(0);
			
			setIsAllocated(false);
	
	}

	 
	protected int getC_BankStatementLine_ID()
	{
		
		String sql = "SELECT C_BankStatementLine_ID FROM C_BankStatementLine WHERE C_Payment_ID=?";
		
		int id = DB.getSQLValue(get_TrxName(), sql, getC_Payment_ID());
		
		if (id < 0)
			return 0;
		return id;
	}	//	getC_BankStatementLine_ID
	
	protected boolean voidOnlinePayment() 
	{
		if (getTenderType().equals(TENDERTYPE_CreditCard) && isOnline())
		{
			setOrig_TrxID(getR_PnRef());
			setTrxType(TRXTYPE_Void);
			if(!processOnline())
			{
				setTrxType(TRXTYPE_CreditPayment);
				if(!processOnline())
				{
					log.log(Level.SEVERE, "Failed to cancel payment online");
					m_processMsg = Msg.getMsg(getCtx(), "PaymentNotCancelled");
					return false;
				}
			}
		}

		if (getC_Invoice_ID() != 0)
		{
			MInvoice inv = new MInvoice(getCtx(), getC_Invoice_ID(), get_TrxName());
			inv.setC_Payment_ID(0);
			inv.saveEx();
		}
		if (getC_Order_ID() != 0)
		{
			MOrder ord = new MOrder(getCtx(), getC_Order_ID(), get_TrxName());
			ord.setC_Payment_ID(0);
			ord.saveEx();
		}
		
		return true;
	}
	
	@Override
	public boolean reverseAccrualIt() 
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		StringBuilder info = reverse(true);
		if (info == null) {
			return false;
		}
		
		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
				
		m_processMsg = info.toString();
		return true;
	}
	
	@Override
	public boolean reverseCorrectIt() 
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		StringBuilder info = reverse(false);
		if (info == null) {
			return false;
		}
		
		// After reverseCorrect
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		
		if (m_processMsg != null)
			return false;

		m_processMsg = info.toString();
		return true;
	
	}
	
	protected StringBuilder reverse(boolean accrual) {
		if (!voidOnlinePayment())
			return null;
		
		//	Std Period open?
		Timestamp dateAcct = accrual ? Env.getContextAsDate(getCtx(), "#Date") : getDateAcct();
		if (dateAcct == null) {
			dateAcct = new Timestamp(System.currentTimeMillis());
		}
		MPeriod.testPeriodOpen(getCtx(), dateAcct, getC_DocType_ID(), getAD_Org_ID());
		
		//	Create Reversal
		NIMPayment reversal = new NIMPayment (getCtx(), 0, get_TrxName());
		copyValues(this, reversal);
		reversal.setClientOrg(this);
		// reversal.setC_Order_ID(0); // IDEMPIERE-1764
		reversal.setC_Invoice_ID(0);
		reversal.setDateAcct(dateAcct);
		//
		reversal.setDocumentNo(getDocumentNo() + REVERSE_INDICATOR);	//	indicate reversals
		reversal.setDocStatus(DOCSTATUS_Drafted);
		reversal.setDocAction(DOCACTION_Complete);
		//
		reversal.setPayAmt(getPayAmt().negate());
		reversal.setDiscountAmt(getDiscountAmt().negate());
		reversal.setWriteOffAmt(getWriteOffAmt().negate());
		reversal.setOverUnderAmt(getOverUnderAmt().negate());
		//
		reversal.setIsAllocated(true);
		reversal.setIsReconciled(false);
		reversal.setIsOnline(false);
		reversal.setIsApproved(true); 
		reversal.setR_PnRef(null);
		reversal.setR_Result(null);
		reversal.setR_RespMsg(null);
		reversal.setR_AuthCode(null);
		reversal.setR_Info(null);
		reversal.setProcessing(false);
		reversal.setOProcessing("N");
		reversal.setProcessed(false);
		reversal.setPosted(false);
		reversal.setDescription(getDescription());
		reversal.addDescription("{->" + getDocumentNo() + ")");
		//FR [ 1948157  ] 
		reversal.setReversal_ID(getC_Payment_ID());
		reversal.saveEx(get_TrxName());
	
		//	Post Reversal
		
		if (!reversal.processIt(DocAction.ACTION_Complete))
		
		{
			m_processMsg = "Reversal ERROR: " + reversal.getProcessMsg();
			return null;
		}
		
		reversal.closeIt();
		
		reversal.setDocStatus(DOCSTATUS_Reversed);
		
		reversal.setDocAction(DOCACTION_None);
		
		reversal.saveEx(get_TrxName());

		//	Unlink & De-Allocate
		
		deAllocate(accrual);
		
		setIsAllocated (true);	//	the allocation below is overwritten
		//	Set Status 
		addDescription("(" + reversal.getDocumentNo() + "<-)");
		setDocStatus(DOCSTATUS_Voided);
		setDocAction(DOCACTION_None);
		setProcessed(true);
		 
		//FR [ 1948157  ] 
		setReversal_ID(reversal.getC_Payment_ID());
		save(get_TrxName());
		StringBuilder info = new StringBuilder(reversal.getDocumentNo());

		//	Create automatic Allocation
		MAllocationHdr alloc = new MAllocationHdr (getCtx(), false, 
			getDateTrx(), 
			getC_Currency_ID(),
			Msg.translate(getCtx(), "C_Payment_ID")	+ ": " + reversal.getDocumentNo(), get_TrxName());
		alloc.setAD_Org_ID(getAD_Org_ID());
		alloc.setDateAcct(dateAcct); // dateAcct variable already take into account the accrual parameter
		alloc.saveEx(get_TrxName());

		//	Original Allocation
		MAllocationLine aLine = new MAllocationLine (alloc, getPayAmt(true), 
			Env.ZERO, Env.ZERO, Env.ZERO);
		aLine.setDocInfo(getC_BPartner_ID(), 0, 0);
		aLine.setPaymentInfo(getC_Payment_ID(), 0);
		if (!aLine.save(get_TrxName()))
			log.warning("Automatic allocation - line not saved");
		//	Reversal Allocation
		aLine = new MAllocationLine (alloc, reversal.getPayAmt(true), 
			Env.ZERO, Env.ZERO, Env.ZERO);
		aLine.setDocInfo(reversal.getC_BPartner_ID(), 0, 0);
		aLine.setPaymentInfo(reversal.getC_Payment_ID(), 0);
		if (!aLine.save(get_TrxName()))
			log.warning("Automatic allocation - reversal line not saved");
		
		// added AdempiereException by zuhri
		if (!alloc.processIt(DocAction.ACTION_Complete))
			throw new AdempiereException("Failed when processing document - " + alloc.getProcessMsg());
		// end added
		alloc.saveEx(get_TrxName());
		//			
		info.append(" - @C_AllocationHdr_ID@: ").append(alloc.getDocumentNo());
		
		//	Update BPartner
		if (getC_BPartner_ID() != 0)
		{
			MBPartner bp = new MBPartner (getCtx(), getC_BPartner_ID(), get_TrxName());
			bp.setTotalOpenBalance();
			bp.saveEx(get_TrxName());
		}
		
		return info;
	} 
	
	/**
	 * 	Allocate It.
	 * 	Only call when there is NO allocation as it will create duplicates.
	 * 	If an invoice exists, it allocates that 
	 * 	otherwise it allocates Payment Selection.
	 *	@return true if allocated
	 */
	public boolean allocateIt()
	{
		//	Create invoice Allocation -	See also MCash.completeIt
		if (getC_Invoice_ID() != 0)
		{	
				return allocateInvoice();
		}	
		//	Invoices of a AP Payment Selection
		if (allocatePaySelection())
			return true;
		
		if (getC_Order_ID() != 0)
			return false;
			
		//	Allocate to multiple Payments based on entry
		MPaymentAllocate[] pAllocs = MPaymentAllocate.get(this);
		if (pAllocs.length == 0)
			return false;
		
		MAllocationHdr alloc = new MAllocationHdr(getCtx(), false, 
				getDateTrx(), getC_Currency_ID(), 
				Msg.translate(getCtx(), "C_Payment_ID")	+ ": " + getDocumentNo(), 
				get_TrxName());
		
		
		alloc.setAD_Org_ID(getAD_Org_ID());
		alloc.setDateTrx(getDateTrx());
		alloc.setDateAcct(getDateAcct()); // in case date acct is different from datetrx in payment; IDEMPIERE-1532 tbayen
		if (!alloc.save())
		{
			log.severe("P.Allocations not created");
			return false;
		}
		//	Lines
		for (int i = 0; i < pAllocs.length; i++)
		{
			MPaymentAllocate pa = pAllocs[i];
			
			MAllocationLine aLine = null;
			if (isReceipt())
				aLine = new MAllocationLine (alloc, pa.getAmount(), 
					pa.getDiscountAmt(), pa.getWriteOffAmt(), pa.getOverUnderAmt());
			else
				aLine = new MAllocationLine (alloc, pa.getAmount().negate(), 
					pa.getDiscountAmt().negate(), pa.getWriteOffAmt().negate(), pa.getOverUnderAmt().negate());
			aLine.setDocInfo(pa.getC_BPartner_ID(), 0, pa.getC_Invoice_ID());			
			aLine.setDateTrx(getDateTrx());
			aLine.setPaymentInfo(getC_Payment_ID(), 0);
			 
			if (!aLine.save(get_TrxName()))
				log.warning("P.Allocations - line not saved");
			else
			{
				pa.setC_AllocationLine_ID(aLine.getC_AllocationLine_ID());
				pa.saveEx();
			}
		}
		// added AdempiereException by zuhri
		if (!alloc.processIt(DocAction.ACTION_Complete))
			throw new AdempiereException("Failed when processing document - " + alloc.getProcessMsg());
		// end added
		m_processMsg = "@C_AllocationHdr_ID@: " + alloc.getDocumentNo();
		return alloc.save(get_TrxName());
	}	//	allocateIt

	/**
	 * 	Allocate single AP/AR Invoice
	 * 	@return true if allocated
	 */
	protected boolean allocateInvoice()
	{
		//	calculate actual allocation
		BigDecimal allocationAmt = getPayAmt();			//	underpayment
		if (getOverUnderAmt().signum() < 0 && getPayAmt().signum() > 0)
			allocationAmt = allocationAmt.add(getOverUnderAmt());	//	overpayment (negative)

		MAllocationHdr alloc = new MAllocationHdr(getCtx(), false, 
			getDateTrx(), getC_Currency_ID(),
			Msg.translate(getCtx(), "C_Payment_ID") + ": " + getDocumentNo() + " [1]", get_TrxName());
		alloc.setAD_Org_ID(getAD_Org_ID());
		alloc.setDateAcct(getDateAcct()); // in case date acct is different from datetrx in payment
		alloc.setDateTrx(getDateTrx());
		alloc.saveEx();
		MAllocationLine aLine = null;
		if (isReceipt())
			aLine = new MAllocationLine (alloc, allocationAmt, 
				getDiscountAmt(), getWriteOffAmt(), getOverUnderAmt());
		else
			aLine = new MAllocationLine (alloc, allocationAmt.negate(), 
				getDiscountAmt().negate(), getWriteOffAmt().negate(), getOverUnderAmt().negate());
		aLine.setDocInfo(getC_BPartner_ID(), 0, getC_Invoice_ID());
		aLine.setC_Payment_ID(getC_Payment_ID());
		aLine.setDateTrx(getDateTrx());
		aLine.saveEx(get_TrxName());
		// added AdempiereException by zuhri
		if (!alloc.processIt(DocAction.ACTION_Complete))
			throw new AdempiereException("Failed when processing document - " + alloc.getProcessMsg());
		// end added
		alloc.saveEx(get_TrxName());
		m_justCreatedAllocInv = alloc;
		m_processMsg = "@C_AllocationHdr_ID@: " + alloc.getDocumentNo();
			
		//	Get Project from Invoice
		int C_Project_ID = DB.getSQLValue(get_TrxName(), 
			"SELECT MAX(C_Project_ID) FROM C_Invoice WHERE C_Invoice_ID=?", getC_Invoice_ID());
		if (C_Project_ID > 0 && getC_Project_ID() == 0)
			setC_Project_ID(C_Project_ID);
		else if (C_Project_ID > 0 && getC_Project_ID() > 0 && C_Project_ID != getC_Project_ID())
			log.warning("Invoice C_Project_ID=" + C_Project_ID 
				+ " <> Payment C_Project_ID=" + getC_Project_ID());
		return true;
	}	//	allocateInvoice
	
	/**
	 * 	Allocate Payment Selection
	 * 	@return true if allocated
	 */
	protected boolean allocatePaySelection()
	{
		MAllocationHdr alloc = new MAllocationHdr(getCtx(), false, 
			getDateTrx(), getC_Currency_ID(),
			Msg.translate(getCtx(), "C_Payment_ID")	+ ": " + getDocumentNo() + " [n]", get_TrxName());
		alloc.setAD_Org_ID(getAD_Org_ID());
		alloc.setDateAcct(getDateAcct()); // in case date acct is different from datetrx in payment
		
		String sql = "SELECT psc.C_BPartner_ID, psl.C_Invoice_ID, psl.IsSOTrx, "	//	1..3
			+ " psl.PayAmt, psl.DiscountAmt, psl.DifferenceAmt, psl.OpenAmt "
			+ "FROM C_PaySelectionLine psl"
			+ " INNER JOIN C_PaySelectionCheck psc ON (psl.C_PaySelectionCheck_ID=psc.C_PaySelectionCheck_ID) "
			+ "WHERE psc.C_Payment_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getC_Payment_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				int C_BPartner_ID = rs.getInt(1);
				int C_Invoice_ID = rs.getInt(2);
				if (C_BPartner_ID == 0 && C_Invoice_ID == 0)
					continue;
				boolean isSOTrx = "Y".equals(rs.getString(3));
				BigDecimal PayAmt = rs.getBigDecimal(4);
				BigDecimal DiscountAmt = rs.getBigDecimal(5);
				BigDecimal WriteOffAmt = Env.ZERO;
				BigDecimal OpenAmt = rs.getBigDecimal(7);
				BigDecimal OverUnderAmt = OpenAmt.subtract(PayAmt)
					.subtract(DiscountAmt).subtract(WriteOffAmt);
				//
				if (alloc.get_ID() == 0 && !alloc.save(get_TrxName()))
				{
					log.log(Level.SEVERE, "Could not create Allocation Hdr");
					return false;
				}
				MAllocationLine aLine = null;
				if (isSOTrx)
					aLine = new MAllocationLine (alloc, PayAmt, 
						DiscountAmt, WriteOffAmt, OverUnderAmt);
				else
					aLine = new MAllocationLine (alloc, PayAmt.negate(), 
						DiscountAmt.negate(), WriteOffAmt.negate(), OverUnderAmt.negate());
				aLine.setDocInfo(C_BPartner_ID, 0, C_Invoice_ID);
				aLine.setC_Payment_ID(getC_Payment_ID());
				if (!aLine.save(get_TrxName()))
					log.log(Level.SEVERE, "Could not create Allocation Line");
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "allocatePaySelection", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		//	Should start WF
		boolean ok = true;
		if (alloc.get_ID() == 0)
		{
			if (log.isLoggable(Level.FINE)) log.fine("No Allocation created - C_Payment_ID=" 
				+ getC_Payment_ID());
			ok = false;
		}
		else
		{
			// added Adempiere Exception by zuhri
			if(alloc.processIt(DocAction.ACTION_Complete))
				ok = alloc.save(get_TrxName());
			else
				throw new AdempiereException("Failed when processing document - " + alloc.getProcessMsg());
			// end added by zuhri
			m_processMsg = "@C_AllocationHdr_ID@: " + alloc.getDocumentNo();
		}
		return ok;
	}	//	allocatePaySelection
	
	// IDEMPIERE-2588
	private MAllocationHdr m_justCreatedAllocInv = null;
	public MAllocationHdr getJustCreatedAllocInv() {
		return m_justCreatedAllocInv;
	}
	
}
