package restartServer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JProgressBar;

@SuppressWarnings("serial")
public class ProgressWindow extends Frame {
	
	private Label label;
	private TextArea textArea;
	private JProgressBar progressBar;
	
	

	public ProgressWindow(){
		super("Neustart Twonkymedia Server");
		Dimension dimensionScreen = Toolkit.getDefaultToolkit().getScreenSize();
		int width = 500;
		int height = 300;
		this.setBounds((dimensionScreen.width - width) / 2, (dimensionScreen.height - height) / 2, width, height);
		this.setLayout(new BorderLayout(5, 5));
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent event){
				dispose();
			}
		});
		
		label = new Label(" ");
		
		progressBar = new JProgressBar();
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);
		
		Panel northPanel = new Panel();
		northPanel.setLayout(new BorderLayout(5, 5));
		northPanel.add(label, BorderLayout.NORTH);
		northPanel.add(progressBar, BorderLayout.CENTER);
		northPanel.add(new Panel(), BorderLayout.EAST); //dummy
		northPanel.add(new Panel(), BorderLayout.WEST); //dummy
		this.add(northPanel, BorderLayout.NORTH);
		
		textArea = new  TextArea();
		textArea.setEditable(false);
		this.add(textArea, BorderLayout.CENTER);
		
		this.setVisible(true);
	}
	
	public void setNote(String note){
		label.setText(note);
	}
	
	public void appendText(String text){
		textArea.append(text);
	}
	
	public void stopProgressBar(){
		progressBar.setVisible(false);
	}
}
