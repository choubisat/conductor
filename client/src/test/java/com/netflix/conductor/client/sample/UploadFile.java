package com.netflix.conductor.client.sample;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.tasks.TaskResult.Status;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

public class UploadFile implements Worker {
	
	private String taskDefName;
	private static final String testFileName = "testFile";
	
	public UploadFile(String t) {
		taskDefName = t;
	}

	@Override
	public String getTaskDefName() {
		// TODO Auto-generated method stub
		return taskDefName;
	}

	@Override
	public TaskResult execute(Task task) {
		// TODO Auto-generated method stub
		System.out.printf("Executing %s, id= %s%n", taskDefName, task.getTaskId());
		System.out.printf("Input data =  %s%n", task.getInputData().toString());
		TaskResult result = new TaskResult(task);
		
		if (task.getInputData().containsKey(CommonKeys.keyUserInput))
		{
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) task.getInputData().get(CommonKeys.keyUserInput);
			String user = map.get(CommonKeys.keyUserName).toString();
			System.out.printf("Sent email to %s for uploading a file.\n", user);
			//Register the output of the task
			result.getOutputData().put(CommonKeys.keyMessage, "Sent email to " + user +" for uploading a file.");
			result.getOutputData().put(CommonKeys.keyFileName, testFileName);
			result.setStatus(Status.COMPLETED);
		}
		else
		{
			result.getOutputData().put("Error", "Inputdata is not containing key= " + CommonKeys.keyUserInput);
			result.setStatus(Status.FAILED);
		}
		return result;
	}
	
	/*
	private Label headerLabel;
	private Frame mainFrame;
	private Label statusLabel;
	private Panel controlPanel;
	
	  private void prepareGUI(){
		    mainFrame = new Frame("Java AWT Examples");
		      mainFrame.setSize(400,400);
		      mainFrame.setLayout(new GridLayout(3, 1));
		      mainFrame.addWindowListener(new WindowAdapter() {
		         public void windowClosing(WindowEvent windowEvent){
		            System.exit(0);
		         }        
		      });    
		      headerLabel = new Label();
		      headerLabel.setAlignment(Label.CENTER);
		      statusLabel = new Label();        
		      statusLabel.setAlignment(Label.CENTER);
		      statusLabel.setSize(350,100);

		      controlPanel = new Panel();
		      controlPanel.setLayout(new FlowLayout());

		      mainFrame.add(headerLabel);
		      mainFrame.add(controlPanel);
		      mainFrame.add(statusLabel);
		      mainFrame.setVisible(true);  
		   }

		   private void showFileDialogDemo(){
			   prepareGUI();
		      headerLabel.setText("Control in action: FileDialog"); 

		      final FileDialog fileDialog = new FileDialog(mainFrame,"Select file");
		      Button showFileDialogButton = new Button("Open File");
		      showFileDialogButton.addActionListener(new ActionListener() {
		         @Override
		         public void actionPerformed(ActionEvent e) {
		            fileDialog.setVisible(true);
		            statusLabel.setText("File Selected :" 
		            + fileDialog.getDirectory() + fileDialog.getFile());
		         }
		      });

		      controlPanel.add(showFileDialogButton);
		      mainFrame.setVisible(true);  
		   }
		   */
}
