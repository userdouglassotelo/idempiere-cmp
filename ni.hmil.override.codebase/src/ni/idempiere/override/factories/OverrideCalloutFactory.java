package ni.idempiere.override.factories;

import java.util.ArrayList;
import java.util.List; 

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory; 

public class OverrideCalloutFactory implements IColumnCalloutFactory {

	@Override
	public IColumnCallout[] getColumnCallouts(String tableName,
			String columnName) 
{
		List<IColumnCallout> list = new ArrayList<IColumnCallout>();
		if (tableName.equals("NI_RequisitionLine") 
				&& columnName.equals("C_BPartner_ID"))
	    {
			list.add(null);
		}
		return list != null ? list.toArray(new IColumnCallout[0])
				: new IColumnCallout[0]; 
	}
}
