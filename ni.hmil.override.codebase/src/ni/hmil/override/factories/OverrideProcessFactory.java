package ni.hmil.override.factories;
 
import ni.hmil.override.process.NIBankTransfer;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

public class OverrideProcessFactory implements IProcessFactory{

	@Override
	public ProcessCall newProcessInstance(String className) {

		if("ni.hmil.override.process.NIBankTransfer".equals(className))
			return new NIBankTransfer();
		return null;
	}

}
