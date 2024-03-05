package com.github.waynemerricks.asteriskphone.callinput;

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.github.waynemerricks.asteriskphone.records.Conversation;

/**
 * Shows a table with the conversation history of the active caller
 * @author waynemerricks
 *
 */
public class CallerHistoryPanel {

	/* CLASS VARS */
	private I18NStrings xStrings;
	private Vector<String> columnNames = new Vector<String>();
	private DefaultTableModel tableModel;
	private JTable history;
	
	public CallerHistoryPanel(String language, String country) {
		
		xStrings = new I18NStrings(language, country);
		
		//Create the Table
		buildTableColumns();
		
		tableModel = new DefaultTableModel(){
			
			private static final long serialVersionUID = 1L;

			public boolean isCellEditable(int row, int col){
				return false;
			}
			
			public Class<String> getColumnClass(int column){
				
				return String.class;
				
			}
			
		};
		
		history = new JTable(tableModel){
			
			private static final long serialVersionUID = 1L;

			public Component prepareRenderer(TableCellRenderer renderer, 
					int row, int column){
				
				Component c = super.prepareRenderer(renderer, row, column);
				
				Color backgroundColour = null;
				
				if(row % 2 == 0)//if we're an odd row go green
					backgroundColour = Color.WHITE;
				else
					backgroundColour = new Color(189, 224, 194);
				
				if(c instanceof MultiLineCellRenderer){
					
					//Setup an attribute Set with the wanted background colour
					SimpleAttributeSet bgAttributes = new SimpleAttributeSet();
					StyleConstants.setBackground(bgAttributes, backgroundColour);
					
					//Convert the component to a MultiLineCellRenderer so we can set the Document
					MultiLineCellRenderer textPane = (MultiLineCellRenderer)c;
					textPane.setContentType("text/html"); 
					
					//Set the document to our wanted attributes
					textPane.getStyledDocument().setParagraphAttributes(0, 
								textPane.getDocument().getLength(), bgAttributes, false);
					
					//Make sure the inner border is the same colour so it doesn't look weird
					textPane.setBorder(BorderFactory.createEmptyBorder());
					
					//Set the Altered TextPane to return
					c = textPane;
					
				}else{
					
					JLabel l = (JLabel)c;
					l.setHorizontalAlignment(JLabel.CENTER);
					
					c.setBackground(backgroundColour);
				
				}
				
				return c;
				
			}
			
		};
		history.setRowSelectionAllowed(false);
		history.setAutoCreateRowSorter(true);
		
	}
	
	/**
	 * Gets the table encapsulated in this class
	 * @return
	 */
	public JTable getTable(){
		return history;
	}
	
	/**
	 * Method to set the data of the history tab
	 * @param conversations
	 */
	public void setConversationHistory(Vector<Conversation> conversations){
		
		tableModel.setDataVector(getTableData(conversations), columnNames);
		
		history.getColumnModel().getColumn(0).setPreferredWidth(10);
		history.getColumnModel().getColumn(1).setCellRenderer(new MultiLineCellRenderer());
		
	}
	
	/**
	 * Converts Vector<Conversation> into Vector<Vector<String>> suitable for use
	 * with table model
	 * @param conversations
	 * @return
	 */
	private Vector<Vector<String>> getTableData(Vector<Conversation> conversations){
		
		Vector<Vector<String>> data = new Vector<Vector<String>>();
		
		Iterator<Conversation> iterator = conversations.iterator();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss"); 
		
		while(iterator.hasNext()){
			
			Conversation c = iterator.next();
			
			Vector<String> row = new Vector<String>();
			row.add(sdf.format(c.getTime()));
			row.add(c.getConversation());
			
			data.add(row);
			
		}
		
		return data;
		
	}
	
	private void buildTableColumns(){
		
		columnNames.add(xStrings.getString("CallerHistoryPanel.timeField")); 
		columnNames.add(xStrings.getString("CallerHistoryPanel.conversationField")); 
		
	}

}