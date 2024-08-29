package ni.idempiere.override.factories;

import java.sql.ResultSet; 

import ni.idempiere.override.models.NIMPaySelectionLine;
import ni.idempiere.override.models.NIMPayment;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;

public class OverrideModelFactory implements IModelFactory
{
	@Override
	public Class<?> getClass(String tableName) 
	{
		if(tableName.equals(NIMPayment.Table_Name))
			return NIMPayment.class;
		if(tableName.equals(NIMPaySelectionLine.Table_Name))
			return NIMPaySelectionLine.class;		 
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		if(tableName.equalsIgnoreCase(NIMPayment.Table_Name))
			return new NIMPayment(Env.getCtx(),Record_ID,trxName);
		if(tableName.equalsIgnoreCase(NIMPaySelectionLine.Table_Name))
			return new NIMPaySelectionLine(Env.getCtx(),Record_ID,trxName);
		 
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		if(tableName.equalsIgnoreCase(NIMPayment.Table_Name))
			return new NIMPayment(Env.getCtx(),rs,trxName);
		if(tableName.equalsIgnoreCase(NIMPaySelectionLine.Table_Name))
			return new NIMPaySelectionLine(Env.getCtx(),rs,trxName);
	 
		return null;
	}

}
