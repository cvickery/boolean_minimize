// $Id: Minimize.java,v 1.4 2005/02/21 23:32:15 vickery Exp $
/*
 * Created on Feb 19, 2005
 *
 *  Author:     C. Vickery
 *
 *  Copyright (c) 2005, Queens College of the City University
 *  of New York.  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or
 *  without modification, are permitted provided that the
 *  following conditions are met:
 *
 *      * Redistributions of source code must retain the above
 *        copyright notice, this list of conditions and the
 *        following disclaimer.
 * 
 *      * Redistributions in binary form must reproduce the
 *        above copyright notice, this list of conditions and
 *        the following disclaimer in the documentation and/or
 *        other materials provided with the distribution.  
 * 
 *      * Neither the name of Queens College of CUNY
 *        nor the names of its contributors may be used to
 *        endorse or promote products derived from this
 *        software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 *  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 *  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  $Log: Minimize.java,v $
 *  Revision 1.4  2005/02/21 23:32:15  vickery
 *  Completed GUI.  Don't know how to scroll
 *  within a table cell, and list of covers can
 *  be too long to see sometimes.
 *
 *  Revision 1.3  2005/02/21 04:19:00  vickery
 *  Continuing GUI development.
 *  Fixed TruthTable to give variable names in alphabetic order,
 *  and to sort minterm numbers.
 *
 *  Revision 1.2  2005/02/20 04:24:00  vickery
 *  Started developing GUI.
 *
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

//  Class Minimize
//  ------------------------------------------------------------------
/**
 *  Provides a graphical interface for the MinimizedTable class.
 */
public class Minimize extends JFrame implements ClipboardOwner
{
  //  Program Preferences
  private static final Preferences      PREFERENCES 
                    = Preferences.userNodeForPackage(Minimize.class);

  static final long serialVersionUID = 7207907089312502783L;  
  static int    numInstances    = 1;

  Minimize      thisWindow      = this;
  JPanel        leftSide        = new JPanel(new BorderLayout());
  JPanel        rightSide       = new JPanel(new BorderLayout());
  JSplitPane    lrSplit         = new JSplitPane(
                   JSplitPane.HORIZONTAL_SPLIT, leftSide, rightSide);
  JPanel        tableHolder     = new JPanel();
  JPanel        mintermTablePanel   = new JPanel();
  JPanel        primeImplicantPanel = new JPanel();
  JPanel        ioPanel         = new JPanel(new GridLayout(2,1));
  Vector<String>  entryBoxHistory = new Vector<String>();
  JComboBox     entryBox        = new JComboBox(entryBoxHistory);
  JLabel        minimizedValue  = new JLabel();

  TableModel    blankMinterms  
                    = new BlankTable("Minterm Number", "Product Term");
  TableModel    blankImplicants
                  = new BlankTable("Prime Implicant", "Implied Terms");
  JTable        mintermTable        = new JTable(blankMinterms);
  JScrollPane   mintermScroller     = new JScrollPane(mintermTable);
  JTable        primeImplicantTable = new JTable(blankImplicants);
  JScrollPane   primeImplicantScroller
                                = new JScrollPane(primeImplicantTable);
  JSplitPane    tableSplit      = new JSplitPane(
                                             JSplitPane.VERTICAL_SPLIT,
                              mintermScroller, primeImplicantScroller);
  JTextArea     processLog      = new JTextArea();
  JButton       cloneButton     = new JButton("New Window");
  JButton       exitButton      = new JButton("Exit");

  Clipboard      clipboard
                    = Toolkit.getDefaultToolkit().getSystemClipboard();
  PrintStream   ps              = null;

  //  Constructors
  //  ================================================================
  /**
   *  Construct initial window, which is initialized based on command
   *  line arguments.
   */
  private Minimize(String[] args)
  {
    super();
    //  Connect System.out to the processLog TextArea.  Used to disply
    //  the commentary produced by MinimizedTable.minimizeIt(), which
    //  normally appears on the console.
        ps = new PrintStream(
            new CapturedOutput(new ByteArrayOutputStream()));
    
    //  Create the GUI and display it.
    createGUI();
    Point p = centerIt(this);
    Dimension d = getSize();
    setSize(new Dimension(
            PREFERENCES.getInt("layoutWidth", d.width),
            PREFERENCES.getInt("layoutHeight", d.height)));
    setLocation(new Point(
            PREFERENCES.getInt("layoutX", p.x),
            PREFERENCES.getInt("layoutY", p.y)));
    lrSplit.setDividerLocation(
            PREFERENCES.getInt("lrSplit", -1));
    tableSplit.setDividerLocation(
            PREFERENCES.getInt("tableSplit", -1));
    String hist = "";
    if (!(hist = PREFERENCES.get("history_0", "")).equals(""))
      entryBoxHistory.add(hist);
    if (!(hist = PREFERENCES.get("history_1", "")).equals(""))
      entryBoxHistory.add(hist);
    if (!(hist = PREFERENCES.get("history_2", "")).equals(""))
      entryBoxHistory.add(hist);
    if (!(hist = PREFERENCES.get("history_3", "")).equals(""))
      entryBoxHistory.add(hist);
    setVisible(true);
  }

  /**
   *  Construct additional window, which is initialized with a copy of
   *  the minimized table from which it was cloned.
   */
    private Minimize(
        Point where, Dimension howBig, int splitLR, int splitTables,
        Vector<String> ebHist)
    {
      super();
      ps = new PrintStream(
                     new CapturedOutput(new ByteArrayOutputStream()));
      numInstances++;
      createGUI();
      setSize(howBig);
      setLocation(where);
      lrSplit.setDividerLocation(splitLR);
      tableSplit.setDividerLocation(splitTables);
      entryBoxHistory.addAll(ebHist);
      setVisible(true);
    }

    
  //  createGUI()
  //  ----------------------------------------------------------------
  /**
   *  Creates the gui for either constructor
   */
    private void createGUI()
    {
      //  Layout the visual components
      entryBox.setEditable(true);
      Container contentPane = this.getContentPane();
      entryBox.setBorder(
        new TitledBorder("Enter Boolean Expression or Minterm List:"));
      ioPanel.add(entryBox);
      minimizedValue.setBorder(
          new TitledBorder("Minimized Expression"));
      ioPanel.add(minimizedValue);
      leftSide.add(ioPanel, BorderLayout.NORTH);

      mintermScroller.setBorder( new TitledBorder("Minterms"));
      TableColumn col_0 = mintermTable.getColumnModel().getColumn(0);
      col_0.setHeaderValue("Minterm Number");
      col_0.setPreferredWidth(100);
      TableColumn col_1 = mintermTable.getColumnModel().getColumn(1);
      col_1.setHeaderValue("Product Term");
      col_1.setPreferredWidth(300);
      mintermTable.setFocusable(false);
      
      primeImplicantScroller.setBorder( 
                                 new TitledBorder("Prime Implicants"));
      col_0 = primeImplicantTable.getColumnModel().getColumn(0);
      col_0.setHeaderValue("Prime Implicant");
      col_0.setPreferredWidth(100);
      col_1 = primeImplicantTable.getColumnModel().getColumn(1);
      col_1.setHeaderValue("Terms Covered");
      col_1.setPreferredWidth(300);
      primeImplicantTable.setFocusable(false);

      leftSide.add(tableSplit, BorderLayout.CENTER);

      JPanel buttonHolder = new JPanel(new FlowLayout());
      buttonHolder.add(Box.createHorizontalGlue());
      buttonHolder.add(cloneButton);
      buttonHolder.add(exitButton);
      buttonHolder.add(Box.createHorizontalGlue());
      leftSide.add(buttonHolder, BorderLayout.SOUTH);
      JScrollPane jsp = new JScrollPane(processLog);
      jsp.setBorder(new TitledBorder("Minimization Steps"));
      rightSide.add(jsp, BorderLayout.CENTER);
      processLog.setFocusable(false);
      contentPane.add(lrSplit);

      pack();
      entryBox.requestFocusInWindow();

    //  Set up event handlers
    //  ============================================================
    /*
     *  Exit Button and Window Close decorator.
     */
      addWindowListener(new WindowAdapter()
      {
        public void windowClosing(WindowEvent we)
        {
          closeWindow();
        }});
      exitButton.addActionListener(new ActionListener()
      { public void actionPerformed(ActionEvent ae)
        {
          closeWindow();
        }});
      exitButton.addKeyListener(new KeyAdapter()
      { // Without this, only spacebar activates after tabbing.
        public void keyReleased(KeyEvent ke)
        {
          if (ke.getKeyCode() == KeyEvent.VK_ENTER)
          {
            closeWindow();
          }
        }});

      /*
       *  Clone Button
       */
      cloneButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ae)
        {
          cloneWindow();
        }});
      cloneButton.addKeyListener(new KeyAdapter()
      { // Without this, only spacebar activates after tabbing.
        public void keyReleased(KeyEvent ke)
        {
          if (ke.getKeyCode() == KeyEvent.VK_ENTER)
          {
            cloneWindow();
          }
        }});

    //  ActionListener for entryBox
    //  -------------------------------------------------------------
    /*
     *  Process new string entered by the user.  Add it to the history
     *  if not already there.
     */
      entryBox.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ae)
        {
          if (ae.getActionCommand().equals("comboBoxChanged")) return;
          String str = (String)entryBox.getSelectedItem();
          if ((str == null)||(str.length()==0)) return;
          if (!entryBoxHistory.contains(str))
          {
            entryBox.insertItemAt(str, 0);           
          }
          //  Clear out any previous results
          processLog.setText("");
          processLog.setForeground(Color.BLACK);
          mintermTable.setModel(blankMinterms);
          primeImplicantTable.setModel(blankImplicants);
          
          //  Determine whether entry is an expression or a list of
          //  minterms.
          boolean         entryIsExpression = false;
          StringTokenizer st = new StringTokenizer(str,", ");
          int[]           termList = new int[st.countTokens()];
          int             i = 0;
          try
          {
            while (st.hasMoreTokens())
            {
              termList[i++] = Integer.parseInt(st.nextToken());
            }
          }
          catch (NumberFormatException nfe)
          {
            entryIsExpression = true;
          }
          //  Invoke appropriate constructor
          TruthTable      t         = null;
          MinimizedTable  m         = null;
          try
          {
            if (entryIsExpression)
            {
              t = new TruthTable(str);
              m = new MinimizedTable(str, ps);
            }
            else
            {
              t = new TruthTable(termList);
              m = new MinimizedTable(termList, ps);
            }
            minimizedValue.setText(m.toString());
            clipboard.setContents(
                      new StringSelection(m.toString()), thisWindow);
            mintermTable.setModel(t);
            primeImplicantTable.setModel(m);
          }
          catch (Exception e)
          {
            processLog.setForeground(Color.RED);
            processLog.setText(e.getMessage());
          }
        }
      }
      );
    }

  //  cloneWindow()
  //  ----------------------------------------------------------------
  /**
   *    Allow the user to make another window, to facilitate looking
   *    at two or more minimizations at once.
   */
    private void cloneWindow()
    {
      Point p = getLocation();
      p.x += 10;
      p.y += 10;
      Dimension d = getSize();
      new Minimize
         (
             p, d,
             lrSplit.getDividerLocation(),
             tableSplit.getDividerLocation(),
             entryBoxHistory
         );         
    }
  //  closeWindow()
  //  ----------------------------------------------------------------
  /**
   *  Save layout if this is the last instance.  Then exit.
   */
    private void closeWindow()
    {
      if (--numInstances < 1)
      {
        saveLayout();
        System.exit(0);
      }
      else
      {
        setVisible(false);
      }
    }
  //  centerIt()
  //  ----------------------------------------------------------------
  /**
   *    Centers the object on the screen.
   * 
   *    @param  it  The window to be centered.
   *    @return     A Point giving the new (x,y) position.
   */
    public static Point centerIt( Container it )
    {
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension window = it.getSize();
      int ulx = (screen.width - window.width) / 2;
      int uly = (screen.height - window.height ) / 2;
      it.setLocation(ulx, uly);
      return new Point(ulx, uly);
    }

  //  saveLayout()
  //  --------------------------------------------------------------
    private void saveLayout()
    {
      Point p = getLocationOnScreen();
      Dimension d = getSize();
      PREFERENCES.putInt("layoutX", p.x);
      PREFERENCES.putInt("layoutY", p.y);
      PREFERENCES.putInt("layoutWidth", d.width);
      PREFERENCES.putInt("layoutHeight", d.height);
      PREFERENCES.putInt("lrSplit", lrSplit.getDividerLocation());
      PREFERENCES.putInt("tableSplit", tableSplit.getDividerLocation());
      String[] hist = {"", "", "", ""};
      for (int i = 0; i < Math.min(4,entryBoxHistory.size()); i++)
      {
        hist[i] = entryBoxHistory.elementAt(i);
        if (hist[i] == null) hist[i] = "";
      }
      PREFERENCES.put("history_0", hist[0]);
      PREFERENCES.put("history_1", hist[1]);
      PREFERENCES.put("history_2", hist[2]);
      PREFERENCES.put("history_3", hist[3]);
    }

  //  lostOwnership()
  //  ---------------------------------------------------------------
  /**
   *  Implements the ClipboardOwner interface.
   */
    public void lostOwnership(Clipboard c, Transferable t) { }

  //  main()
  //  ---------------------------------------------------------------
  /**
   *  Instantiate the GUI.
   */
    public static void main(String[] args)
    {
      new Minimize(args);
    }

  //  Class BlankTable
  //  ----------------------------------------------------------------
  /**
   *  Provides a "placeholder" TableModel for the minterm and
   *  implicants tables when no expression has been evaluated.
   */
  private class BlankTable extends AbstractTableModel
  {
    private String col_0 = "Left", col_1 = "Right";
    public BlankTable(String c0, String c1) { col_0 = c0; col_1 = c1; }
    static final long serialVersionUID = -8649950458668140690L;    
    public int getColumnCount() {return 2;}
    public int getRowCount()    {return 5;}
    public boolean getIsEditable(int row, int col) {return false;}
    public String getColumnName(int col)
    {
      switch (col)
      {
        case 0:
          return col_0;
        case 1:
          return col_1;
        default:
          throw new RuntimeException("Bad Switch");
      }
    }
    public Object getValueAt(int row, int col) { return ""; }
  }

  //  Class CapturedOutput
  //  ---------------------------------------------------------------
  /**
   *  Used to interecept print statements and append them to the
   *  processLog TextArea.  In non-GUI mode, they go to System.out.
   */
  private class CapturedOutput extends FilterOutputStream
  {
    public CapturedOutput(OutputStream os)
    {
      super(os);
    }
    public void write(byte[] bArray)
    {
      processLog.append(new String(bArray));
    }
    public void write(byte[] bArray, int off, int len)
    {
      processLog.append(new String(bArray , off , len));
    }
  }
}
