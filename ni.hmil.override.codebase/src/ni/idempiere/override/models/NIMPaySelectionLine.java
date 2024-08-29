package ni.idempiere.override.models;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionLine;

public class NIMPaySelectionLine extends MPaySelectionLine{

	public NIMPaySelectionLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	public NIMPaySelectionLine(MPaySelection ps, int Line, String PaymentRule) {
		super(ps, Line, PaymentRule);
		// TODO Auto-generated constructor stub
	}

	public NIMPaySelectionLine(Properties ctx, int C_PaySelectionLine_ID,
			String trxName) {
		super(ctx, C_PaySelectionLine_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// TODO Auto-generated method stub
		return true;
	}
}
