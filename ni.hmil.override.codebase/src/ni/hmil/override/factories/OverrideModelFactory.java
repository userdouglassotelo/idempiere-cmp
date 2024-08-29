package ni.hmil.override.factories;

import java.sql.ResultSet; 

import ni.hmil.override.models.NIMPayment; 

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;

public class OverrideModelFactory implements IModelFactory{

	@Override
	public Class<?> getClass(String tableName) {
		if(tableName.equals(NIMPayment.Table_Name))
			return NIMPayment.class;
		 
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		if(tableName.equalsIgnoreCase(NIMPayment.Table_Name))
			return new NIMPayment(Env.getCtx(),Record_ID,trxName);
		 
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		if(tableName.equalsIgnoreCase(NIMPayment.Table_Name))
			return new NIMPayment(Env.getCtx(),rs,trxName);
	 
		return null;
	}

}
