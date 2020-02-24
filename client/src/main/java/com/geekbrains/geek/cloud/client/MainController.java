package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    @FXML
    ListView<String> filesServer;

    @FXML
    Label lb1, lb2;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMsg abstractMessage = Network.readObject();
                    if (abstractMessage instanceof FileMsg) {
                        FileMsg fileMsg = (FileMsg) abstractMessage;
                        Files.write(Paths.get("client/client_storage/" + fileMsg.getFilename()), fileMsg.getBytes(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    } else if (abstractMessage instanceof CommandMsg) {
                        CommandMsg commandMsg = (CommandMsg) abstractMessage;
                        String [] listFromServer = commandMsg.getCommand().split(" ");
                        Platform.runLater(() -> {
                            filesServer.getItems().addAll(listFromServer);
                        });
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
    }

    public void pressOnUploadBtn(ActionEvent actionEvent) throws IOException {
        if (tfFileName.getLength() > 0) {
            if (Files.exists(Paths.get("client/client_storage/" + tfFileName.getText()))) {
                FileMsg fileMsg = new FileMsg(Paths.get("client/client_storage/" + tfFileName.getText()));
                Network.sendMsg(fileMsg);
                tfFileName.clear();
            }
        }
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) throws IOException {
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new CommandMsg("download", tfFileName.getText()));
            tfFileName.clear();
            refreshLocalFilesList();
        }
    }

    public void pressOnCommandBtn(ActionEvent actionEvent) {
        filesServer.getItems().clear();
        Network.sendMsg(new CommandMsg("list", tfFileName.getText()));
    }

    public void pressOnExitBtn(ActionEvent actionEvent) {
        Network.sendMsg(new CommandMsg("exit", tfFileName.getText()));
        Network.stop();
        Platform.exit();
    }

    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client/client_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void selectFile (MouseEvent mouseEvent){
        if(mouseEvent.getClickCount()==2){
            Platform.runLater(() -> {
               String str = filesList.getSelectionModel().getSelectedItems().toString();
               str=str.substring(1, str.length()-1);
               tfFileName.setText (str);
            });
        }
    }

    public void selectFileServer (MouseEvent mouseEvent){
        if(mouseEvent.getClickCount()==2){
            Platform.runLater(() -> {
                String str = filesServer.getSelectionModel().getSelectedItems().toString();
                str=str.substring(1, str.length()-1);
                tfFileName.setText (str);
            });
            //           MiniStage ms = new MiniStage(clientsList.getSelectionModel().getSelectedItems(), out, textArea);
        }
    }
}
