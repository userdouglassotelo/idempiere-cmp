package ni.hmil.override.models;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.util.PaymentUtil;
import org.compiere.model.MBankAccount;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MSysConfig;
import org.compiere.model.Obscure;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

public class NIMPayment extends MPayment 
{ 
	CLogger log=CLogger.getCLogger(NIMPayment.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -8248599698974482671L;

	public NIMPayment(Properties ctx, int C_Payment_ID, String trxName) {
		super(ctx, C_Payment_ID, trxName);
		
	}
	
	public NIMPayment(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord) 
	
	{
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
	             || is_ValueChanged(COLUMNNAME_WriteOffAmt))) 
		{
			
				log.saveError("PaymentAlreadyProcessed", Msg.translate(getCtx(), "C_Payment_ID"));
		
				return false;
	
		}
			// @Trifon - CashPayments
			//if ( getTenderType().equals("X") ) {
			
		if ( isCashbookTrx()) {
				// Cash Book Is mandatory
		
			if ( getC_CashBook_ID() <= 0 ) {
					//log.saveError("Error", Msg.parseTranslation(getCtx(), "@Mandatory@: @C_CashBook_ID@"));
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
			if (getC_Charge_ID() != 0 && this.isPrepayment() == false) 
			{
				if (newRecord || is_ValueChanged("C_Charge_ID"))
				{
					setC_Order_ID(0);
					setC_Invoice_ID(0);
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
			
		/*
			if (newRecord
                    || is_ValueChanged("C_Charge_ID") || is_ValueChanged("C_Invoice_ID")
                    
                   || is_ValueChanged("C_Order_ID") || is_ValueChanged("C_Project_ID"))
                         setIsPrepayment (isPrepayment() && getC_Charge_ID() == 0
                          && getC_BPartner_ID() != 0
                               && (getC_Order_ID() == 0
                               || (getC_Project_ID() == 0 && getC_Invoice_ID() == 0)));
	*/
			
			/*if (newRecord 
		 			|| is_ValueChanged("C_Charge_ID") || is_ValueChanged("C_Invoice_ID")
		 			|| is_ValueChanged("C_Order_ID") || is_ValueChanged("C_Project_ID"))
		///			setIsPrepayment (getC_Charge_ID() == 0 
					setIsPrepayment (isPrepayment() && getC_Charge_ID() == 0 
		 				&& getC_BPartner_ID() != 0
		///				&& (getC_Order_ID() != 0 
		///					|| (getC_Project_ID() != 0 && getC_Invoice_ID() == 0)));
						&& (getC_Order_ID() == 0 
							|| (getC_Project_ID() == 0 && getC_Invoice_ID() == 0)));*/
		 	
			/*if (isPrepayment())
		 		{
		 			if (newRecord 
			
			///if (isPrepayment())
			
			////{
			///	if (newRecord 
					|| is_ValueChanged("C_Order_ID") || is_ValueChanged("C_Project_ID") && this.isPrepayment() == false)
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
				if (getC_Invoice_ID() != 0) 
				{
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
	protected boolean afterSave(boolean newRecord, boolean success) {
		 
		return super.afterSave(newRecord, success);
	} 
	
	
	
	
	
	
}
