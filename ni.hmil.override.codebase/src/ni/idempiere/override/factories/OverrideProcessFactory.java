package ni.idempiere.override.factories;

import ni.idempiere.override.process.NIBankTransfer;
import ni.idempiere.override.process.NIImportOrder;
import ni.idempiere.override.process.NIInOutCreateInvoice;

import org.adempiere.base.IProcessFactory; 
import org.compiere.process.ProcessCall;
import org.compiere.util.CLogger;

public class OverrideProcessFactory implements IProcessFactory{

	private final static CLogger log = CLogger.getCLogger(OverrideProcessFactory.class);
	@Override
	public ProcessCall newProcessInstance(String className) {

		if("ni.idempiere.override.process.NIBankTransfer".equals(className))
			return new NIBankTransfer();
		if("ni.idempiere.override.process.NIInOutCreateInvoice".equals(className))
			return new NIInOutCreateInvoice();
		if("ni.idempiere.override.process.NIImportOrder".equals(className))
			return new NIImportOrder();
		return null;
	}

}
