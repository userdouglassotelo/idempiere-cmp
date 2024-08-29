package ni.cmp.override.form;

import org.adempiere.webui.apps.form.WSQLProcess;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.CLogger;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;

public class WSQLProcess extends CustomFormController

{

	private static final long serialVersionUID = -625685961198219434L;
	
	 private static final CLogger  log = CLogger.getCLogger(WSQLProcess.class);

	    /** Grid used to layout components. */
	    private Grid m_grdMain = new Grid();
	    /** SQL label. */
	    private Label m_lblSql = new Label("SQL");
	    /** SQL statement field. */
	    private Textbox m_txbSqlField = new Textbox();
	    /** Process button. */
	    private Button m_btnSql = createProcessButton();
	    /** Field to hold result of SQL statement execution. */
	    private Textbox m_txbResultField = new Textbox();

	@Override
	protected void initForm() 
	{
		
		Row rwTop = new Row();
        Row rwBottom = new Row();
        Rows rows = new Rows();
        final int noColumns = 60;
        final int maxStatementLength = 9000;
        final int noStatementRows = 3;
        final int noResultRows = 20;

        ZKUpdateUtil.setWidth(m_grdMain, "80%");

        // create the top row of components
        m_txbSqlField.setMultiline(true);
        m_txbSqlField.setMaxlength(maxStatementLength);
        m_txbSqlField.setRows(noStatementRows);
        ZKUpdateUtil.setHflex(m_txbSqlField, "1");
        m_txbSqlField.setCols(noColumns);
        m_txbSqlField.setReadonly(false);

        m_btnSql.addEventListener(Events.ON_CLICK, this);

        rwTop.appendChild(m_lblSql);
        rwTop.appendChild(m_txbSqlField);
        rwTop.appendChild(m_btnSql);

        rows.appendChild(rwTop);

        // create the bottom row of components
        m_txbResultField.setCols(noColumns);
        m_txbResultField.setRows(noResultRows);
        ZKUpdateUtil.setHflex(m_txbResultField, "1");
        m_txbResultField.setReadonly(true);

        rwBottom.appendCellChild(m_txbResultField, 3);
        rwBottom.setAlign("center");

        rows.appendChild(rwBottom);

        // put it all together
        m_grdMain.appendChild(rows);

        Borderlayout contentPane = new Borderlayout();
		this.appendChild(contentPane);
		ZKUpdateUtil.setWidth(contentPane, "99%");
		ZKUpdateUtil.setHeight(contentPane, "100%");
		Center center = new Center();
		center.setStyle("border: none");
		contentPane.appendChild(center);
		ZKUpdateUtil.setHflex(m_grdMain, "true");
		ZKUpdateUtil.setVflex(m_grdMain, "true");
		center.appendChild(m_grdMain);

        return;
		
		
	}

	private Button createProcessButton() 
	{
		
		return null;
	}


}
