package ni.cmp.override.form;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;

import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Datebox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.ListCell;
import org.adempiere.webui.component.ListHead;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.ToolBarButton;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MElementValue;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MOrg;
import org.compiere.model.MTable;
import org.compiere.model.MTreeNode;
import org.compiere.model.Query;
import org.compiere.report.MReportTree;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.ValueNamePair;
import org.zkoss.image.AImage;
import org.zkoss.image.Image;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Center;
import org.zkoss.zul.Frozen;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;




public class WSQLProcess extends ADForm implements EventListener<Event>
{
	
	private static final long serialVersionUID = 6463330405100383354L;
	
	private CustomForm mForm = new CustomForm();
	
	/** Log. */
    
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
	
  
    public static String processStatements (String sqlStatement)
	{
		String instruccionSQL = sqlStatement;
		
		
		//	Process
		Connection conn = DB.createConnection(true, Connection.TRANSACTION_READ_COMMITTED);
		Statement stmt = null;
		try
		{
			stmt = conn.createStatement(); 
			
			System.out.println(instruccionSQL);
			stmt.executeUpdate(instruccionSQL);

		}
		catch (SQLException e)
		{
			e.printStackTrace();

		}
		
		
		try
		{
			stmt.close();
		}
		catch (SQLException e1)
		{
			e1.printStackTrace();
		
		}
		stmt = null;
		try
		{
			conn.close();
		}
		catch (SQLException e2)
		{
			e2.printStackTrace();
			
		}
		conn = null;
		
		return instruccionSQL;
	}	
    
	private Button createProcessButton() 
	{
		  Button btnProcess = new Button();
	        if(ThemeManager.isUseFontIconForImage())
	        	btnProcess.setIconSclass("z-icon-Process");
	        else
	        	btnProcess.setImage(ThemeManager.getThemeResource("images/Process24.png"));
	        btnProcess.setName(Msg.getMsg(Env.getCtx(), "Process"));

	        return btnProcess;
	}
	
	
	@Override
	public void onEvent(Event event) throws Exception 
	{
		
		if (event.getTarget() == m_btnSql)
    		m_txbResultField.setText(processStatements (m_txbSqlField.getText()));
		super.onEvent(event);
		
	}

	@Override
	protected void initForm() {
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
		//this.appendChild(contentPane);
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
}
