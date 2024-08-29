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

public class NIPrepayment extends MPayment {

	public NIPrepayment(Properties ctx, int C_Payment_ID, String trxName)
	{
		super(ctx, C_Payment_ID, trxName);
		
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
