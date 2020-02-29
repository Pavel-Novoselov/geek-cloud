package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final Logger admin = Logger.getLogger("admin");
    private boolean isAuthorized;
    private boolean isRegistered;

    @FXML
    Label textArea;

    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    @FXML
    ListView<String> filesServer;

    @FXML
    VBox bottomPanel;

    @FXML
    VBox upperPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    TextField nickFieldReg;

    @FXML
    TextField loginFieldReg;

    @FXML
    PasswordField passwordFieldReg;

    @FXML
    VBox firstPanel;

    @FXML
    Label lb7, lb1, lb2, lb3, lb4, lb5, lb6;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        admin.info("Соединение с сервером установлено");
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
                        if (commandMsg.getCommand().startsWith("List: ")) {
                            String[] listFromServer = commandMsg.getCommand().substring(6).split(", ");
                            Platform.runLater(() -> {
                                filesServer.getItems().clear();
                                filesServer.getItems().addAll(listFromServer);
                            });
                        }
                        if (commandMsg.getCommand().startsWith("RegOK")) {
                            setAuthrizedAndReg(false, true);
                        }
                        if (commandMsg.getCommand().startsWith("AuthOK")) {
                            setAuthrizedAndReg(true, true);
                            admin.info("Авторизация прошла успешно!");
                        }
                        if (commandMsg.getCommand().startsWith("Неверный логин/пароль")) {
                            setAuthrizedAndReg(false, true);
                            Platform.runLater(() -> {
                                textArea.setText("неверный логин/пароль!!!");
                            });
                            admin.warn("неверный логин/пароль!!!");
                        }
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

    public  void pressOnDeleteClientBtn (ActionEvent actionEvent){
        if (tfFileName.getLength() > 0) {
            Path path = Paths.get("client/client_storage/"+ tfFileName.getText()).toAbsolutePath();
            if (Files.exists(path)){
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Fine does not exist");
                }
            }
            tfFileName.clear();
            refreshLocalFilesList();
        }
    }

    public  void pressOnDeleteServerBtn (ActionEvent actionEvent){
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new CommandMsg("delete", tfFileName.getText()));
            tfFileName.clear();
  //          refreshLocalFilesList();
        }
    }

    public void pressOnCommandBtn(ActionEvent actionEvent) {
        filesServer.getItems().clear();
        Network.sendMsg(new CommandMsg("list"));
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

    public void selectFile(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            Platform.runLater(() -> {
                String str = filesList.getSelectionModel().getSelectedItems().toString();
                str = str.substring(1, str.length() - 1);
                tfFileName.setText(str);
            });
        }
    }

    public void selectFileServer(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            Platform.runLater(() -> {
                String str = filesServer.getSelectionModel().getSelectedItems().toString();
                str = str.substring(1, str.length() - 1);
                tfFileName.setText(str);
            });
            //           MiniStage ms = new MiniStage(clientsList.getSelectionModel().getSelectedItems(), out, textArea);
        }
    }

    private void setAuthrizedAndReg(boolean isAuthorized, boolean isRegistered) {
        this.isAuthorized = isAuthorized;
        this.isRegistered = isRegistered;
        if ((!isRegistered) && (!isAuthorized)) {
            firstPanel.setVisible(true);
            firstPanel.setManaged(true);
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
        } else if ((isRegistered) && (!isAuthorized)) {
            firstPanel.setVisible(false);
            firstPanel.setManaged(false);
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
        } else if ((isRegistered) && (isAuthorized)) {
            firstPanel.setVisible(false);
            firstPanel.setManaged(false);
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        Network.sendMsg(new CommandMsg("auth", loginField.getText() + " " + passwordField.getText()));
    }

    //отсылаем форму регистрации
    public void tryToReg(ActionEvent actionEvent) {
        Network.sendMsg(new CommandMsg("reg", nickFieldReg.getText() + " " + loginFieldReg.getText() + " " + passwordFieldReg.getText()));
    }

    public void goToAuth(ActionEvent actionEvent) {
        isRegistered = true;
        isAuthorized = false;
        firstPanel.setVisible(false);
        firstPanel.setManaged(false);
        upperPanel.setVisible(true);
        upperPanel.setManaged(true);
        bottomPanel.setVisible(false);
        bottomPanel.setManaged(false);
    }


}
