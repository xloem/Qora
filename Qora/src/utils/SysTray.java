package utils;

import gui.ConsolePanel;
import gui.Gui;
import gui.PasswordPane;
import gui.QoraRowSorter;
import gui.SendMessagePanel;
import gui.SendMoneyPanel;
import gui.assets.AssetsPanel;
import gui.models.WalletTransactionsTableModel;
import gui.naming.NamingServicePanel;
import gui.settings.SettingsFrame;
import gui.transaction.TransactionDetailsFactory;
import gui.voting.VotingPanel;

import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import qora.transaction.Transaction;
import settings.Settings;
import controller.Controller;
import database.wallet.TransactionMap;

public class SysTray {

	private static SysTray systray = null;
	private TrayIcon icon = null;
	private PopupMenu createPopupMenu;

	public static SysTray getInstance() {
		if (systray == null) {
			systray = new SysTray();
		}

		return systray;
	}

	public void createTrayIcon() throws HeadlessException,
			MalformedURLException, AWTException, FileNotFoundException {
		if (icon == null) {
			if (!SystemTray.isSupported()) {
				System.out.println("SystemTray is not supported");
			} else {
				createPopupMenu = createPopupMenu();
				TrayIcon icon = new TrayIcon(createImage(
						"images/icons/icon32.png", "tray icon"), "Qora "
						+ Controller.getInstance().getVersion(),
						createPopupMenu);
				
				icon.setImageAutoSize(true);
				
				SystemTray.getSystemTray().add(icon);
				this.icon = icon;
				
				icon.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							Gui.getInstance().bringtoFront();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				});
			}
		}
	}
	
	
	public void sendMessage(String caption, String text, TrayIcon.MessageType messagetype  )
	{
		if(icon != null)
		{
			icon.displayMessage(caption, text,
					messagetype);
		}
	}

	// Obtain the image URL
	private  Image createImage(String path, String description)
			throws MalformedURLException, FileNotFoundException {

		File file = new File(path);

		if (!file.exists()) {
			throw new FileNotFoundException("Iconfile not found: " + path);
		}

		URL imageURL = file.toURI().toURL();
		return (new ImageIcon(imageURL, description)).getImage();
	}

	private PopupMenu createPopupMenu() throws HeadlessException {
		PopupMenu menu = new PopupMenu();
		
		
		MenuItem decentralizedWeb = new MenuItem("Qora Web/Social Network");
		decentralizedWeb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
        			URLViewer.openWebpage(new URL("http://127.0.0.1:"+Settings.getInstance().getWebPort()));
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
			}
		});
		menu.add(decentralizedWeb);
		MenuItem walletStatus = new MenuItem("Wallet Status (web)");
		walletStatus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					URLViewer.openWebpage(new URL("http://127.0.0.1:"+Settings.getInstance().getWebPort() + "/index/status.html"));
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
			}
		});
		menu.add(walletStatus);
		
		
		
		MenuItem lock = new MenuItem("Lock/Unlock");
		lock.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(Controller.getInstance().isWalletUnlocked())
				{
					Controller.getInstance().lockWallet();
				}else
				{
					String password = PasswordPane.showUnlockWalletDialog(); 
					if(!password.equals("") && !Controller.getInstance().unlockWallet(password))
					{
						JOptionPane.showMessageDialog(null, "Invalid password", "Unlock Wallet", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		menu.add(lock);
		
		
		MenuItem settings = new MenuItem("Settings");
		settings.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				new SettingsFrame();
			}
		});
		menu.add(settings);
		
		
		MenuItem console = new MenuItem("Console");
		console.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
					JFrame frame = new JFrame("Console");

				    frame.setSize(800, 600);
				    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				    frame.setVisible(true);
					ConsolePanel ap = new ConsolePanel();
					frame.getContentPane().add(ap);
					
			}
		});
		menu.add(console);
		
		MenuItem transactions = new MenuItem("Transactions");
		transactions.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
					JFrame frame = new JFrame("Transactions");

				    frame.setSize(800, 600);
				    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				    frame.setVisible(true);
				 final   WalletTransactionsTableModel transactionsModel = new WalletTransactionsTableModel();
				final    JTable transactionsTable = new JTable(transactionsModel);
					
					//TRANSACTIONS SORTER
					Map<Integer, Integer> indexes = new TreeMap<Integer, Integer>();
					indexes.put(WalletTransactionsTableModel.COLUMN_CONFIRMATIONS, TransactionMap.TIMESTAMP_INDEX);
					indexes.put(WalletTransactionsTableModel.COLUMN_TIMESTAMP, TransactionMap.TIMESTAMP_INDEX);
					indexes.put(WalletTransactionsTableModel.COLUMN_ADDRESS, TransactionMap.ADDRESS_INDEX);
					indexes.put(WalletTransactionsTableModel.COLUMN_AMOUNT, TransactionMap.AMOUNT_INDEX);
					QoraRowSorter sorter = new QoraRowSorter(transactionsModel, indexes);
					transactionsTable.setRowSorter(sorter);
					
					//TRANSACTION DETAILS
					transactionsTable.addMouseListener(new MouseAdapter() 
					{
						public void mouseClicked(MouseEvent e) 
						{
							if(e.getClickCount() == 2) 
							{
								//GET ROW
						        int row = transactionsTable.getSelectedRow();
						        row = transactionsTable.convertRowIndexToModel(row);
						        
						        //GET TRANSACTION
						        Transaction transaction = transactionsModel.getTransaction(row);
						         
						        //SHOW DETAIL SCREEN OF TRANSACTION
						        TransactionDetailsFactory.getInstance().createTransactionDetail(transaction);
						    }
						}
					});			
					
					frame.getContentPane().add(new JScrollPane(transactionsTable)  );
					
			}
		});
		menu.add(transactions);
		
		MenuItem payment = new MenuItem("Send Payment");
		payment.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
					JFrame frame = new JFrame("Send Payment");

				    frame.setSize(800, 600);
				    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				    frame.setVisible(true);
					SendMoneyPanel ap = new SendMoneyPanel();
					frame.getContentPane().add(ap);
					
			}
		});
		menu.add(payment);
		
		MenuItem messages = new MenuItem("Messages");
		messages.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
					JFrame frame = new JFrame("Messages");

				    frame.setSize(800, 600);
				    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				    frame.setVisible(true);
					SendMessagePanel ap = new SendMessagePanel();
					frame.getContentPane().add(ap);
					
			}
		});
		menu.add(messages);

		MenuItem assets = new MenuItem("Assets");
		assets.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
					JFrame frame = new JFrame("Assets");

				    frame.setSize(800, 600);
				    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				    frame.setVisible(true);
					AssetsPanel ap = new AssetsPanel();
					frame.getContentPane().add(ap);
					
			}
		});
		menu.add(assets);
		
		MenuItem names = new MenuItem("Names");
		names.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
					JFrame frame = new JFrame("Names");

				    frame.setSize(800, 600);
				    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				    frame.setVisible(true);
					frame.getContentPane().add(
							new NamingServicePanel());
					
			}
		});
		menu.add(names);
		
		MenuItem voting = new MenuItem("Voting");
		voting.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
					JFrame frame = new JFrame("Voting");

				    frame.setSize(800, 600);
				    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				    frame.setVisible(true);
					frame.getContentPane().add(
							new VotingPanel());
					
			}
		});
		menu.add(voting);
		
		MenuItem exit = new MenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				 Controller.getInstance().stopAll();
	              System.exit(0);
			}
		});
		menu.add(exit);
		
		return menu;
	}
}