package ni.hmil.override.factories;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;

public class OverrideCalloutFactory implements IColumnCalloutFactory
{

	@Override
	public IColumnCallout[] getColumnCallouts(String tableName,
			String columnName) {
		return null;
	}
}
