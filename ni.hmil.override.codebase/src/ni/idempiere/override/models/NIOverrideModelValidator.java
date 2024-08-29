package ni.idempiere.override.models; 

import java.math.BigDecimal;
import java.util.logging.Level;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.osgi.service.event.Event;  
import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_Payment;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment; 
import org.compiere.model.PO; 
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

public class NIOverrideModelValidator extends AbstractEventHandler {
	private static CLogger log = CLogger.getCLogger(NIOverrideModelValidator.class);
	
	@Override
	protected void initialize()   {
		// TODO Auto-generated method stub
		
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MPayment.Table_Name);		
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE,  MPayment.Table_Name);  
		
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MAllocationLine.Table_Name);   
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MAllocationHdr.Table_Name);   
		
	}
	
	@SuppressWarnings("unused")
	@Override
	protected void doHandleEvent(Event event) 
	{ 
		
		String type = event.getTopic();
		PO po = getPO(event);  
		
		
		if(type.equals(IEventTopics.DOC_BEFORE_COMPLETE) && 
				(po instanceof NIMPayment || po instanceof MPayment))
		
		{
			String msg = onCompletePayment(po);
		
			/*if (msg != null) 
			{
				log.log(Level.SEVERE, msg);			
				throw new RuntimeException(msg);
			}*/  
		}
		
		else if(type.equals(IEventTopics.PO_BEFORE_NEW) && (po instanceof MAllocationLine) )
		
		{ 
			Integer paymentid=(Integer) po.get_Value(MPayment.COLUMNNAME_C_Payment_ID);
			
			Integer invoiceid=(Integer) po.get_Value(MPayment.COLUMNNAME_C_Invoice_ID); 
			
			
			MInvoice invoice = null;
			
			Integer C_Campaign_ID,C_Project_ID;
			
			C_Project_ID =(Integer) po.get_Value(MPayment.COLUMNNAME_C_Project_ID);
			
			C_Campaign_ID=(Integer) po.get_Value(MPayment.COLUMNNAME_C_Campaign_ID); 
			 
			if((paymentid==null && invoiceid==null) || (C_Project_ID!=null && C_Project_ID.intValue()>0 
					&& C_Campaign_ID!=null && C_Campaign_ID.intValue()>0))
				return;
			
			MPayment payment=new MPayment(Env.getCtx(),paymentid==null?0:paymentid, po.get_TrxName()); 
			
			if(payment==null)
				invoice=new MInvoice(Env.getCtx(),invoiceid==null?0:invoiceid, po.get_TrxName());			 
			
			C_Campaign_ID=(payment!=null && payment.getC_Payment_ID()>0)?payment.getC_Campaign_ID():
				(invoice!=null && invoice.getC_Invoice_ID()>0)?invoice.getC_Campaign_ID():0;
			C_Project_ID=(payment!=null && payment.getC_Payment_ID()>0)?payment.getC_Project_ID():
				(invoice!=null && invoice.getC_Invoice_ID()>0)?invoice.getC_Project_ID():0;
			
			
			if(C_Campaign_ID!=null && C_Campaign_ID.intValue()>0) 
				if(po.get_ColumnIndex(MPayment.COLUMNNAME_C_Campaign_ID)!=-1) 						
					po.set_CustomColumn(MPayment.COLUMNNAME_C_Campaign_ID,C_Campaign_ID );  
			
			if(C_Project_ID!=null && C_Project_ID.intValue()>0) 					
				if(po.get_ColumnIndex(MPayment.COLUMNNAME_C_Project_ID)!=-1)  
						po.set_CustomColumn(MPayment.COLUMNNAME_C_Project_ID,C_Project_ID);  
				  
		}
		
		else if(type.equals(IEventTopics.DOC_BEFORE_COMPLETE) && (po instanceof MAllocationHdr) )
		
		{ 
		
			MAllocationHdr ahd=(MAllocationHdr)po;
			
			Integer C_Project_ID =(Integer) po.get_Value(MPayment.COLUMNNAME_C_Project_ID);
			Integer C_Campaign_ID=(Integer) po.get_Value(MPayment.COLUMNNAME_C_Campaign_ID); 
			
			
			if(C_Project_ID!=null && C_Project_ID.intValue()>0 
					&& C_Campaign_ID!=null && C_Campaign_ID.intValue()>0)
				return;			
			
			
			MAllocationLine[] lines=ahd.getLines(false);
			
			if(lines==null)
				throw new AdempiereException("!!!ERROR AL GUARDAR PROYECTO Y CENTRO DE COSTO EN LA ASIGNACI�N.!!!");
			
			MAllocationLine line=lines[0];

			C_Campaign_ID=(Integer) line.get_Value(MPayment.COLUMNNAME_C_Campaign_ID);
			C_Project_ID=(Integer) line.get_Value(MPayment.COLUMNNAME_C_Project_ID); 
			
			if(C_Campaign_ID!=null && C_Campaign_ID.intValue()>0) 
				if(ahd.get_ColumnIndex(MPayment.COLUMNNAME_C_Campaign_ID)!=-1) 						
					ahd.set_CustomColumn(MPayment.COLUMNNAME_C_Campaign_ID,C_Campaign_ID );  
			
			if(C_Project_ID!=null && C_Project_ID.intValue()>0) 					
				if(ahd.get_ColumnIndex(MPayment.COLUMNNAME_C_Project_ID)!=-1)  
					ahd.set_CustomColumn(MPayment.COLUMNNAME_C_Project_ID,C_Project_ID);  
			
			
		}
		
	}

	private String  onCompletePayment(PO po)
	{
		MPayment payment=null; 
		
		Integer reversalid=(Integer) po.get_Value("Reversal_ID");
		
		if((reversalid==null || reversalid.intValue()==0))
		{
			
			payment=(MPayment) po; 
			
			int no=DB.getSQLValue(payment.get_TrxName(),
					"SELECT COUNT(*) FROM C_PaymentAllocate WHERE C_Payment_ID="+payment.getC_Payment_ID());
			
			
			if(payment.getC_Charge_ID()==0 && no==0)
			{
				String msg=Msg.getMsg(Env.getCtx(), "ErrorOnCompletePayment");  
				payment.setErrorMessage(msg); 
				return msg;
			} 
		
			if(payment.getPayAmt().compareTo(BigDecimal.ZERO)==0 || 
					payment.getPayAmt().compareTo(BigDecimal.ZERO)==-1){
				return "!!!Error al completar este Documento!!!. El monto de este debe ser mayor a cero.";
			}
		} 
		return null;
	}

	
}
