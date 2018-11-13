package application;
	
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("Sample.fxml"));
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			
			scene.addEventHandler(KeyEvent.KEY_PRESSED, (key)->{
				if(key.getCode()==KeyCode.W) 
					Cont.up = true;
				if(key.getCode()==KeyCode.S) 
					Cont.down = true;
				if(key.getCode()==KeyCode.A) 
					Cont.left = true;
				if(key.getCode()==KeyCode.D) 
					Cont.right = true;
			});
			scene.addEventHandler(KeyEvent.KEY_RELEASED, (key)->{
				if(key.getCode()==KeyCode.W) 
					Cont.up = false;
				if(key.getCode()==KeyCode.S) 
					Cont.down = false;
				if(key.getCode()==KeyCode.A) 
					Cont.left = false;
				if(key.getCode()==KeyCode.D) 
					Cont.right = false;
			});
			
			primaryStage.setScene(scene);
			primaryStage.setResizable(false);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
