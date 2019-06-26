/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - confirmation dialog on vault removal
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Cell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.ui.ExitUtil;
import org.cryptomator.ui.controls.DirectoryListCell;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.AppLaunchEvent;
import org.cryptomator.ui.model.AutoUnlocker;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.VaultFactory;
import org.cryptomator.ui.model.VaultList;
import org.cryptomator.ui.model.upgrade.UpgradeStrategies;
import org.cryptomator.ui.model.upgrade.UpgradeStrategy;
import org.cryptomator.ui.util.DialogBuilderUtil;
import org.cryptomator.ui.util.Tasks;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static org.cryptomator.ui.util.DialogBuilderUtil.buildErrorDialog;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19vNXVp+NLiHeZNBuQljNtSXP8C6wPMAjGcjLTgG/5x7nbtVikkqUrf
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
import static org.cryptomator.ui.util.Constants.MASTERPASS_FILENAME;
import static org.cryptomator.ui.util.Constants.VAULTID_FILENAME;
import java.security.SecureRandom;
import java.math.BigInteger;
import org.cryptomator.ui.util.SecureCharSequence;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.ui.util.PKI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import org.cryptomator.ui.util.VaultState;
import org.apache.commons.io.FileUtils;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

@FxApplicationScoped
public class MainController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
	private static final String ACTIVE_WINDOW_STYLE_CLASS = "active-window";
	private static final String INACTIVE_WINDOW_STYLE_CLASS = "inactive-window";
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18R8CJM96fpAXZrq+3YGOWPeUKXM1AYIhvwNi2y2mlZCIGXhKkKjVYA
ixxmFgPdWFIvFYIZIREBUg==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private static VaultState.State vstate = VaultState.State.UNDEFINED;

	private final Stage mainWindow;
	private final ExitUtil exitUtil;
	private final Localization localization;
	private final ExecutorService executorService;
	private final BlockingQueue<AppLaunchEvent> launchEventQueue;
	private final VaultFactory vaultFactoy;
	private final ViewControllerLoader viewControllerLoader;
	private final ObjectProperty<ViewController> activeController = new SimpleObjectProperty<>();
	private final ObservableList<Vault> vaults;
	private final BooleanBinding areAllVaultsLocked;
	private final ObjectProperty<Vault> selectedVault = new SimpleObjectProperty<>();
	private final ObjectExpression<Vault.State> selectedVaultState = ObjectExpression.objectExpression(EasyBind.select(selectedVault).selectObject(Vault::stateProperty));
	private final BooleanExpression isSelectedVaultValid = BooleanExpression.booleanExpression(EasyBind.monadic(selectedVault).map(Vault::isValidVaultDirectory).orElse(false));
	private final BooleanExpression canEditSelectedVault = selectedVaultState.isEqualTo(Vault.State.LOCKED);
	private final MonadicBinding<UpgradeStrategy> upgradeStrategyForSelectedVault;
	private final BooleanBinding isShowingSettings;
	private final Map<Vault, UnlockedController> unlockedVaults = new HashMap<>();
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/fw0nvjJgvnejXvLMf7AvKYdf1FDSYHmomtjhvcSC5gD+SCkk8XnI0
1+KQD2rFojA8yiv7CoPlQw==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private final Optional<KeychainAccess> keychainAccess;

	private Subscription subs = Subscription.EMPTY;

	@Inject
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19DPcENjBbw8NivZrtbuoWsPIwDhe1JnNRHQk5GGf1js90Y2s3yx/ZI
iMFAPNfJyFsuL/W7aijrezJF3IDAqCbhuBnwf/U9BU0lxI4jDhFNK4tOqZUMy5Mg
9f1etig3k9PthaBsyF2RpOZZcwIXzn+mT9C6mDAJYEUhHN8O3ezEQ1PmkZ9PWbo9
5DH0uOZ5IgIEoPRq7BkaiMoXafvhkI+vlXKVzC87Xa4iHcR+BKRBA1GhmdR+9TBi
wd8fSjnmV7WQts5TEcM1sFDMREthXpezGtkdKvOA8aXN0HyesmNbdCzQR9vJUjeJ
FOnssj6eFTsUdqP3S60oz2AAxF6bsqk1qecWN5Dc9PoeckigOkNnTOIrxrDoWbIw
u5+6FExAK5nCAqfsVI9ZFmzum4XhTr/pDpuf9hgpjL7F585EXztRYev/KQd4v4lt
CNw3ijEr8/6lVfCv0vatZN3PIfyGzM49cl+lwY+GflCw33eF2s73DQvntPOfvXJU
q+A+iz7wI6qnszZWKrm8HmjYMkoE4EsPQC6JO3d4sc1Dh4Gpgu5UASkPkYCr2Wkm
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	public MainController(@Named("mainWindow") Stage mainWindow, ExecutorService executorService, @Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue, ExitUtil exitUtil, Localization localization,
						  VaultFactory vaultFactoy, ViewControllerLoader viewControllerLoader, UpgradeStrategies upgradeStrategies, VaultList vaults, AutoUnlocker autoUnlocker, Optional<KeychainAccess> keychainAccess) {
		this.mainWindow = mainWindow;
		this.executorService = executorService;
		this.launchEventQueue = launchEventQueue;
		this.exitUtil = exitUtil;
		this.localization = localization;
		this.vaultFactoy = vaultFactoy;
		this.viewControllerLoader = viewControllerLoader;
		this.vaults = vaults;
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18LMqqQn3SfA1GVGA7FbaL2MjGa0y7+NAoDhtigZzm2JQ85zmxM7r//
xW1xt9uTCuKDfMuQxBKx9w==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		this.keychainAccess = keychainAccess;

		// derived bindings:
		this.isShowingSettings = Bindings.equal(SettingsController.class, EasyBind.monadic(activeController).map(ViewController::getClass));
		this.upgradeStrategyForSelectedVault = EasyBind.monadic(selectedVault).map(upgradeStrategies::getUpgradeStrategy);
		this.areAllVaultsLocked = Bindings.isEmpty(FXCollections.observableList(vaults, Vault::observables).filtered(Vault.NOT_LOCKED));

		EasyBind.subscribe(areAllVaultsLocked, exitUtil::updateTrayIcon);
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19QqElF2xMVeq/tEHS7kZnlRzlOZnLzPJYzTJ/e0zQTpWfsq0BCkOby
NWcX17VzHkSxBVqW8OHCtbDtyJ1SeqIgAnAOxhULhytVbNIZyubqwu5P5H1MTi3Y
AsiYxc06fNxGjEqxpdsudPsbJxzjmFLAl4HXe4/5dvfDGtDDP0XUSI50Qvctld9r
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		Platform.setImplicitExit(false);
		autoUnlocker.unlockAllSilently();

		try {
			Desktop.getDesktop().setPreferencesHandler(e -> {
				Platform.runLater(this::toggleShowSettings);
			});
		} catch (UnsupportedOperationException e) {
			LOG.info("Unable to setPreferencesHandler, probably not supported on this OS.");
		}
	}

	@FXML
	private ContextMenu vaultListCellContextMenu;

	@FXML
	private MenuItem changePasswordMenuItem;

	@FXML
	private ContextMenu addVaultContextMenu;

	@FXML
	private HBox root;

	@FXML
	private ListView<Vault> vaultList;

	@FXML
	private ToggleButton addVaultButton;

	@FXML
	private Button removeVaultButton;

	@FXML
	private ToggleButton settingsButton;

	@FXML
	private Pane contentPane;

	@FXML
	private Pane emptyListInstructions;

	@Override
	public void initialize() {
		vaultList.setItems(vaults);
		vaultList.getSelectionModel().clearSelection();
		vaultList.setOnKeyReleased(this::didPressKeyOnList);
		vaultList.setCellFactory(this::createDirecoryListCell);
		root.setOnKeyReleased(this::didPressKeyOnRoot);
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18X4IN3Y2KcyGPrApzbXBePHVBk/CHe5SSaHk0kRCM9m6jdpGtiuDFo
OnjjWZv4UANekuj1BKsD5jmeKphgcujc1188RapeYkRaxSNusYu6wp/WzvzHaO5D
TZRoUheTnWaJe6Ds6aTzxzB8FfUoZFSgbdqhzL5FwlSl/IMj9hctD9op26BrFclg
QYU0iTVS1BziDiP8oqUnX5VgAv2oKfmN1zjzwgo6Jgk=
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		showWelcomeView();
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+MRQ4wIoEeYoH/crM3YdIpG49YfAbFEKg5mpngG5u86JNMvNrBsDbt
3OO1/MqN1Lyx3AKqDyy3/GQTEpy0ibZHY8ZxyWGJ1Egu+JvCWpKwiFKEcTJbhG7S
qE+nC7fLeAzUDuE6laUtHK5AW4nLjtOyqu2+zLcxU6FQHj7gbo8FnRwaax+QMLCk
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		removeVaultButton.setVisible(false);
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/bJTdnk3we23JsvK9t5hrKx+gA8C1Z5simHQSuiVEujVq5wIFt8D75
CrDjLVHNFHO3EcoQ+y2DVRqcFUSTduLqY/8rDHJQfmlMyv9tD8zeLZyEI7T6PFXT
aHBpX9cR6Yj/gwJsNaDer6ok88f5MJyN0PGmJUteJinGb7OS/UrItH42ulOkEDHP
LP8IgOMHQoCgsv8uh9/KpA==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		emptyListInstructions.setVisible(false);
		changePasswordMenuItem.visibleProperty().bind(isSelectedVaultValid.and(Bindings.isNull(upgradeStrategyForSelectedVault)));
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18Q69fx2MqxebeTzKrP4X9b/FS21+OSQoXLIS2CYPFa59SmUX7raYF7
y9zQp2ShZDyfVzqn6Nx6FA==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		addVaultButton.setVisible(false);

		subs = subs.and(EasyBind.subscribe(selectedVault, this::selectedVaultDidChange));
		subs = subs.and(EasyBind.subscribe(activeController, this::activeControllerDidChange));
		subs = subs.and(EasyBind.subscribe(isShowingSettings, settingsButton::setSelected));
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+2FBz2Znr29eBCACpnPsQpkdmMQ+1eLRJPD17tkOyBGcylVw8Lz6wn
FEWNmy9V1JR92N3KKQ/6fktO+QqYxIG8/ESrETuRNWnCjyPz2m0XIatKKvFl3YKe
lo5Bi8gj5W+v+rlSOMSyvbY0/x0lkY6UCEvnXKxJRF6l6N5lmqv9x95r2kxyKmwF
3sKaPFE4K1oZoBiTkPUImZr4gSCma7eWvC1C0SHDFWQ=
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	public void initStage(Stage stage) {
		stage.setScene(new Scene(getRoot()));
		stage.sizeToScene();
		stage.setTitle(localization.getString("app.name")); // set once before bind to avoid display bugs with Linux window managers
		stage.titleProperty().bind(windowTitle());
		stage.setResizable(false);
		loadFont("/css/ionicons.ttf");
		loadFont("/css/fontawesome-webfont.ttf");
		if (SystemUtils.IS_OS_MAC_OSX) {
			subs = subs.and(EasyBind.includeWhen(mainWindow.getScene().getRoot().getStyleClass(), ACTIVE_WINDOW_STYLE_CLASS, mainWindow.focusedProperty()));
			subs = subs.and(EasyBind.includeWhen(mainWindow.getScene().getRoot().getStyleClass(), INACTIVE_WINDOW_STYLE_CLASS, mainWindow.focusedProperty().not()));
			Application.setUserAgentStylesheet(getClass().getResource("/css/mac_theme.css").toString());
		} else if (SystemUtils.IS_OS_LINUX) {
			stage.getIcons().add(new Image(getClass().getResourceAsStream("/window_icon_512.png")));
			Application.setUserAgentStylesheet(getClass().getResource("/css/linux_theme.css").toString());
		} else if (SystemUtils.IS_OS_WINDOWS) {
			stage.getIcons().add(new Image(getClass().getResourceAsStream("/window_icon_32.png")));
			Application.setUserAgentStylesheet(getClass().getResource("/css/win_theme.css").toString());
		}
		exitUtil.initExitHandler(() -> Platform.runLater(this::gracefulShutdown));
		listenToFileOpenRequests(stage);
	}

	private void gracefulShutdown() {
		vaults.filtered(Vault.NOT_LOCKED).forEach(Vault::prepareForShutdown);
		if (!vaults.filtered(Vault.NOT_LOCKED).isEmpty()) {
			mainWindow.show(); // to keep the application open
			ButtonType tryAgainButtonType = new ButtonType(localization.getString("main.gracefulShutdown.button.tryAgain"));
			ButtonType forceShutdownButtonType = new ButtonType(localization.getString("main.gracefulShutdown.button.forceShutdown"));
			Alert gracefulShutdownDialog = DialogBuilderUtil.buildGracefulShutdownDialog(
					localization.getString("main.gracefulShutdown.dialog.title"), localization.getString("main.gracefulShutdown.dialog.header"), localization.getString("main.gracefulShutdown.dialog.content"),
					forceShutdownButtonType, ButtonType.CANCEL, forceShutdownButtonType, tryAgainButtonType);
			Optional<ButtonType> choice = gracefulShutdownDialog.showAndWait();
			choice.ifPresent(btnType -> {
				if (tryAgainButtonType.equals(btnType)) {
					gracefulShutdown();
				} else if (forceShutdownButtonType.equals(btnType)) {
					Platform.runLater(Platform::exit);
				} else {
					if (!vaults.filtered(Vault.NOT_LOCKED).isEmpty()) {
						showUnlockedView(vaults.get(0), false); //if there are still unlocked vaults, show one of them
					} else {
						showUnlockView(UnlockController.State.UNLOCKING); //otherwise show any vault
					}
				}
			});
		} else {
			Platform.runLater(Platform::exit);
		}
	}

	private void loadFont(String resourcePath) {
		try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
			Font.loadFont(in, 12.0);
		} catch (IOException e) {
			LOG.warn("Error loading font from path: " + resourcePath, e);
		}
	}

	private void listenToFileOpenRequests(Stage stage) {
		Tasks.create(launchEventQueue::take).onSuccess(event -> {
			stage.setIconified(false);
			stage.show();
			stage.toFront();
			stage.requestFocus();
			event.getPathsToOpen().forEach(path -> addVault(path, true));
		}).schedulePeriodically(executorService, Duration.ZERO, Duration.ZERO);
	}

	private ListCell<Vault> createDirecoryListCell(ListView<Vault> param) {
		final DirectoryListCell cell = new DirectoryListCell();
		cell.setVaultContextMenu(vaultListCellContextMenu);
		cell.setOnMouseClicked(this::didClickOnListCell);
		return cell;
	}

	// ****************************************
	// UI Events
	// ****************************************

	@FXML
	private void didClickAddVault() {
		if (addVaultContextMenu.isShowing()) {
			addVaultContextMenu.hide();
		} else {
			addVaultContextMenu.show(addVaultButton, Side.BOTTOM, 0.0, 0.0);
		}
	}

	@FXML
	private void didClickCreateNewVault() {
		final FileChooser fileChooser = new FileChooser();
		final File file = fileChooser.showSaveDialog(mainWindow);
		if (file == null) {
			return;
		}
		try {
			final Path vaultDir = file.toPath();
			if (Files.exists(vaultDir)) {
				try (Stream<Path> stream = Files.list(vaultDir)) {
					if (stream.filter(this::isNotHidden).findAny().isPresent()) {
						buildErrorDialog( //
								localization.getString("main.createVault.nonEmptyDir.title"), //
								localization.getString("main.createVault.nonEmptyDir.header"), //
								localization.getString("main.createVault.nonEmptyDir.content"), //
								ButtonType.OK).show();
						return;
					}
				}
			} else {
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/NlPwO8OKADCUluzHRZvBvzjtgp8EGSlQ3PhmIYeVjZbTrWvegLgcr
ODbVZMbNjNfaFE6hypZ24opCOCjYPUDnSBHKtYLT//gt9yVnJBd8Je6ysquGzqxu
SuiZKi++IpuN0/IkFyrMyUR//ZB2UmtCDnF4EDMl8UNWlzgd0ikd/8vhKKSFm5eA
By20gdj6uBYgxN7qI/Q/eFQzyvNy9p5vZnZuomUnASE=
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				Files.createDirectories(vaultDir);
			}
			addVault(vaultDir, true);
		} catch (IOException e) {
			LOG.error("Unable to create vault", e);
		}
	}

	private boolean isNotHidden(Path file) {
		return !file.getFileName().toString().startsWith(".");
	}

	@FXML
	private void didClickAddExistingVaults() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		final List<File> files = fileChooser.showOpenMultipleDialog(mainWindow);
		if (files != null) {
			for (final File file : files) {
				addVault(file.toPath(), true);
			}
		}
	}

	/**
	 * adds the given directory or selects it if it is already in the list of directories.
	 *
	 * @param path to a vault directory or masterkey file
	 */
	public void addVault(final Path path, boolean select) {
		final Path vaultPath;
		if (path != null && Files.isDirectory(path)) {
			vaultPath = path;
		} else if (path != null && Files.isReadable(path)) {
			vaultPath = path.getParent();
		} else {
			LOG.warn("Ignoring attempt to add vault with invalid path: {}", path);
			return;
		}

		final Vault vault = vaults.stream().filter(v -> v.getPath().equals(vaultPath)).findAny().orElseGet(() -> {
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19xrmHZVaQVRRB3X1Yfz4t5ZNPXrSjtukbD0BygAtk2awWdEdlyw+f/
9TkPIUJIC0BXC2Kyg1wplGRM77Dmp697qdlaTaK2CciNRLn7ZdJ+q7YMW99Rg6yC
15hWfLO3066kGJpiwZIBnQVAdd+Nh8JHG184dHJf12mHB+TVd7A1Y6rrM0fpEOlc
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
    		final VaultSettings vaultSettings;
    		switch (vstate) {
    		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19D5vKZSdX7s/e1Xo30ZzCS7EQPnBXltSzaXey0PvUc7MPIhqZ0Vy9R
n8RzjbrOzzs6P7Kgv+D4FtlOxywI1KWHIYNEnFTrCfreoT07W7d7x9v/pu0qN5GQ
    		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
        	case NEW:
        		LOG.info("Vault state is NEW. Using a new random vault ID.");
        		vaultSettings = VaultSettings.withRandomId();
        		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19qLYenxn+CZnQNVNhaNIgzwH30SxekLk5iz5lSROfhfC3SlnlWZRdX
h85GS1ZfB9dkWcfaHeu3ShjJzqFbebKdcAYCcClADSk=
        		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				try {
					LOG.debug("Saving vault ID into VAULTID_FILENAME.");
					final Path vaultidfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + VAULTID_FILENAME);
	            	Files.write(vaultidfile, vaultSettings.getId().getBytes(StandardCharsets.US_ASCII), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				} catch (Exception ex) {
					LOG.warn("Unable to save vault ID.");
				}
        		break;
    		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+iXslox98JNmDSmNMd2vd/Ghc7CGR8oGjtyzY76p3/Qhshoa1Yrt7p
DjasVMhTjQv/sN+ts6UfsNAK1pCINvIyKidiWhsBvJ3EtNJlrt8dsR4UFZ54/Okq
UWIFssGmtCMtQRUxW3iiKQ==
    		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
        	case RUN:
        		LOG.info("Vault state is RUN. Using existing vault ID from vault directory.");
        		String vaultid = new String("");
        		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+7l2djdAVmtZiTYvjuUvXC3aZck1/yaC2O8kNI1Alip7eBEIWGXyvV
8LmiswEt//SO+RuXiW4q8La/HV6moHLuTqlWDbAoBqU=
        		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				try {
					final Path vaultidfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + VAULTID_FILENAME);
	            	vaultid = new String(Files.readAllBytes(vaultidfile), StandardCharsets.US_ASCII).trim();
				} catch (Exception ex) {
					LOG.warn("Unable to load vault ID.");
				}
				if (vaultid.isEmpty()) {
					LOG.info("Empty vault ID from vaultid.virtualsafe.");
        			vaultSettings = VaultSettings.withRandomId();
				} else {
					LOG.debug("Vault ID from vaultid.virtualsafe: " + vaultid);
        			vaultSettings = new VaultSettings(vaultid);
				}
        		break;
        	default:
        		LOG.error("Abnormal. State is unknown during addVault() processing.");
        		vaultSettings = VaultSettings.withRandomId();
        		break;
        	}
			vaultSettings.path().set(vaultPath);
			return vaultFactoy.get(vaultSettings);
		});

		if (!vaults.contains(vault)) {
			vaults.add(vault);
		}
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+1O0UKoKqAgmIyn76lucSOhkyzuZchVOJBAUwlfOi3T+LFKZHukb9d
2vn+kI+47IM4qe6dTJK/nsjDPXcyJmIgmPDfOjAKf1ukleemMAUY717mP2j5Lfo6
xcdDLDFak+aE0sB8FvhiYc6Jm7hafvLQUkTmDApLQU4N53wq3eD8GXYISMy67S1h
lQMlmeL2XkWZccNuD+lRqpmPLXZJy6Q58or45XKEKwF1+b9R+NtC/Z2poTi9MrxU
9kyNR76ZCiFdxpVOLoafhCDcW0j7kUt9lGdl64nLpojros+bSkmYQhjT15uuu8fZ
XkGx7EYw0E3tSZu8nqM9UZ2EN3hZ2/0FjXb/xvKg7geZ60ZpUam3UnTxHojE6gRN
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		Platform.runLater(() -> {
			vaultList.getSelectionModel().select(vault);
		});
	}

	@FXML
	private void didClickRemoveSelectedEntry() {
		Alert confirmDialog = DialogBuilderUtil.buildConfirmationDialog( //
				localization.getString("main.directoryList.remove.confirmation.title"), //
				localization.getString("main.directoryList.remove.confirmation.header"), //
				localization.getString("main.directoryList.remove.confirmation.content"), //
				SystemUtils.IS_OS_MAC_OSX ? ButtonType.CANCEL : ButtonType.OK);

		Optional<ButtonType> choice = confirmDialog.showAndWait();
		if (ButtonType.OK.equals(choice.get())) {
			vaults.remove(selectedVault.get());
			if (vaults.isEmpty()) {
				activeController.set(viewControllerLoader.load("/fxml/welcome.fxml"));
			} else {
				activeController.get().focus();
			}
		}
	}

	@FXML
	private void didClickChangePassword() {
		showChangePasswordView();
	}

	@FXML
	private void didClickShowSettings() {
		toggleShowSettings();
	}

	private void toggleShowSettings() {
		if (isShowingSettings.get()) {
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/pwvWi4lnMjYZhPU+QixunKD4JSwJrMI9siUdnwinwzMtmUv9ro4vU
q5UJdVPhMpkYwIoe32LeNcT7KNwhfrP+CCsB4YIJY3yQ+eowIXw/wZAuHGpWFbW1
da5a/2Tjx3Md/b2YjWkYzA==
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			if (!vaults.isEmpty()) {
				vaultList.getSelectionModel().select(vaults.get(0));
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+rRQ3URojm/6ykaA8NF9Jp5jZ/LU9ogwRUm84IuWa0jtRZNrw+cX/2
1bI3sqHI9y3mU7rjXppnqyoLAvlfMPtdOoLY/+L4rHHsERh9WT4k1Zhity0Tx+GH
/RB5DUmMH9Nq62dnleTxNKPyfG39DSMJLtqxjgGS4wk=
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				return;
			} else {
				showWelcomeView();
			}
		} else {
			showPreferencesView();
		}
		vaultList.getSelectionModel().clearSelection();
	}

	// ****************************************
	// Binding Listeners
	// ****************************************

	private void activeControllerDidChange(ViewController newValue) {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+vdeYBECQjxYisAT3qglxJQGcVygB0AHn+yA1r0tRyNncUwOU+6AlY
Vb6Nj7nTjPc0kyxJPOTNsI5DfYBzDG5oEnU4uQ6qPL7fSbJyP/VMmK6XRqSOM2lB
SP94HI+AQpsI+X7+t2CN53fkN9xqxhIlwfHJKEwN5G55i3CcZRMqWyu7kjin4u1w
ZaQKVNg4PaneDSnxULIpH+4tskoJfvWOlwspbimJVJYzP/bbCZ/dVBJ2FmmsBRkp
hn23z5rj0N143W7R7zSLON0ywV0oiUmoKFl451keoMfvC08HLIdXyIppxQ0DzccF
kMLEU6fDANSYxRLqwZ4iYtIna+zhmH6TghZdbXNIB/0jN/vtRvcpbeYOaxAcPrB4
4uCV2J/cGz7JllSWd2U8bPCaiSe6AeQIlh+KKqmKBROo7+zJs0xchQDdnprQXHsU
uVly6NmIQHwNYo0/pVpIogmbRxF6oTA0g3/Rq8a92YyefBhN8WnQKdn08nrfF7Yz
KUdR0uY8/pzWu/vQzrsuEIaD7VgPuIGSB0dx70Eov5HA5xgxzDAmVBMscEZ6udb8
etJaXU5lCOwC8ykR9jNatg==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		Platform.runLater(() -> {
			final Parent root = newValue.getRoot();
			contentPane.getChildren().clear();
			contentPane.getChildren().add(root);
		});
	}

	private void selectedVaultDidChange(Vault newValue) {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+nBK4TTf6QmaQIuqNZaihiInOpvoGUuzM/NKDOHFKeAxlaHzoqFa07
ruDM4PC1TVFxUxSITQtLB1yWip/HopmcgybG2OiJ2xc=
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		LOG.debug("selectedVaultDidChange()");
		if (newValue == null) {
			LOG.debug("selectedVaultDidChange() newValue == null");
			return;
		}
		if (newValue.getState() != Vault.State.LOCKED) {
			LOG.debug("selectedVaultDidChange() Vault state not LOCKED, showUnlockedView");
			this.showUnlockedView(newValue, false);
		} else if (!newValue.doesVaultDirectoryExist()) {
			LOG.debug("selectedVaultDidChange() !doesVaultDirectoryExist, showNotFoundView");
			LOG.warn("Vault state is RUN and present in settings file but vault directory is missing.");
			this.showNotFoundView();
		} else if (newValue.isValidVaultDirectory() && upgradeStrategyForSelectedVault.isPresent()) {
			LOG.debug("selectedVaultDidChange() showUpgradeView");
			this.showUpgradeView();
		} else if (newValue.isValidVaultDirectory()) {
			LOG.debug("selectedVaultDidChange() showUnlockView");
			this.showUnlockView(UnlockController.State.UNLOCKING);
		} else {
			LOG.debug("selectedVaultDidChange() showInitializeView");
			this.showInitializeView();
		}
	}

	private void didPressKeyOnList(KeyEvent e) {
		if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
			activeController.get().focus();
		}
	}

	private void didPressKeyOnRoot(KeyEvent event) {
		boolean triggered;
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+4p/XDbkbLCwODtiVEX3mZPPxaCMOSgegvAVNLoUc/USel60Q1Z5Hr
ATsvtoBW6BG9wopms/C77g==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		/*
		if (SystemUtils.IS_OS_MAC) {
			triggered = event.isMetaDown();
		} else {
			triggered = event.isControlDown() && !event.isAltDown();
		}
		if (triggered && event.getCode().isDigitKey()) {
			int digit = Integer.valueOf(event.getText());
			switch (digit) {
				case 0: {
					vaultList.getSelectionModel().clearSelection();
					showWelcomeView();
					return;
				}
				default: {
					vaultList.getSelectionModel().select(digit - 1);
					activeController.get().focus();
					return;
				}
			}
		}
		*/
	}

	private void didClickOnListCell(MouseEvent e) {
		if (MouseEvent.MOUSE_CLICKED.equals(e.getEventType()) && e.getSource() instanceof Cell && ((Cell<?>) e.getSource()).isSelected()) {
			activeController.get().focus();
		}
	}

	// ****************************************
	// Public Bindings
	// ****************************************

	public Binding<String> windowTitle() {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+E8jw1V8/2lSYPlejQsMCI0IYadiVrpELwXW0QDnpdxRzN9uax4IjH
Qt0/9dQAD8IzWzpVZKqYnht4YnSnfG95/EWlQ9g6pFBahdNL7GEID9gt+HGbuQa6
cM/X98J3gmaIR8BgDsnzy/wRBUXfwFWVmov+41pUNpjkZSMfFjnJ5H5MKQVLAqrt
0Hb3izxtj0zaNl7BauhODI1fApH22rWd/smdZ6QtGVw=
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		return EasyBind.monadic(selectedVault).flatMap(Vault::mountName).orElse("VirtualSAFE");
	}

	// ****************************************
	// Subcontroller for right panel
	// ****************************************

	private void showWelcomeView() {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/aWr8urPEeKAa/1SUP6FYRF6aA2J4ye/E14/St/CLvbKeYIdDvBbZ3
9ENlaRrb3bCpeZTRigQbWPNAQZXUOSAdICcZgpsW4h7gcK0Ik8IljUvPq/kx1ISL
WuivhPNnvyadiHlLIWARL1IoKQyH2OYxJlOjKAp4zSGTQRkKlJCZQ+EzAPx5v/Xl
gEnQ4viPqfceoJIaBGtpBg==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		final WelcomeController ctrl = viewControllerLoader.load("/fxml/welcome.fxml");
		ctrl.setListener(this::didWelcome);
		activeController.set(ctrl);
	}

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+2yZb3hkoJwz48q2J3JVlhexDr0xnD/8vK4HOk5zTIaE/eKuxxNAin
HwNPvx1A0EPWmk1PQx2kCKTVwwndbGv5/Gc8o8n3/aI=
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	public void didWelcome(VaultState.State state) {
    	vstate = state;
    	boolean err = false;
    	switch (state) {
        case NEW:
        	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+ywwENJ+aB3lUviPWisTB+24UPYL80UWew1Yd8KKneELf6G0FDOsDr
Rcuue7Qti1LEO8FFB0SU9q8D1uRvI7sP8bL2Mt2rnttT3kvIgRZNMUCCVuTkxgtx
EURasijpnkOYyNhus4U1snwEx/2Ga1dyZyzqaDGVRjo=
        	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			if (vaults.isEmpty()) {
				LOG.info("Vault state is NEW. No existing vault in settings file. Creating new vault.");
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX180Ibd1ZigFOURELW8+pG0VwEGQ5cuE2jn+3zXaR+tHBEFOsa3S8Vg1
xXS7SFzsu0ydJXFxXZqrFxICT87YATOHmKO1Wv6Xcb4KEeYqYWCbDbDNIH4m/pXR
UTU23yBOsNAN1VQpTunIBQo9eCiKvyuo7Cp4Lls7GLvUO4qsZ2U11k9MU/jPeVZp
y+X0yys44ftDsoJCPexoD2UzK3QV0LlXExWB7nnllOwQ+FejhU3odfoCTyXbZ7P7
/DTJiarm7t8+XHtqZsJzukypFXp4ArJPlLutn+x6MXyY/QpvOOYB50UP0oRdZ9ru
Z/1BxTL6ztk8w9fk7MBN9COa1NwJ55tyIzerC8liyrPkf8MoNeFJol6a/GAYM6Lg
Ja87wDmXvn7SmUpguVTCMw==
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX194VXZo54JGPT4RqKHpajYXZVlpyCygxwkNeD3U6nYFbK4zwPCOZs7z
rFSUkCBr5hiQfcHkz/7ULR4QL0lobVUQ9O2UDzOSfvLKeY+RceZ9Kb+5ydeIdjhM
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				try {
					final Path vaultDir = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE");
					String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
					final Path backupVaultDir = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC_conflict" + System.getProperty("file.separator") + timestamp + System.getProperty("file.separator"));
					if (!Files.exists(vaultDir)) {
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/UpYvGt/t7du29cRzxamtdmzDJt/RuABOHHvOLhipHQk19qn5mglt0
Cy1Mt1PEIVBamOhj7RS5Kpkkme7l7qDFHM7zhM/CyoFxRpI9wo08NEGd9t0BIeLJ
tnG5kBgUExoOd0zNbuob5zKuUTt9USXzltqs9fcANHrBbxwvEe8ll+qaezZkuE7U
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						Files.createDirectories(vaultDir);
					} else {
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+ZhAaW1DfH1mRJY0oG+Xv5o6Naqh3LnM9IYgKFwbLVYtzOJ9soGNCL
TXNLMwzvNo4wX/DKLUjM6aqPs3CPN2T2Crw+3kzsUQzA5PAeAWOPJhgi+YpsVPDy
DvbjDTsYyBDt2LJolzkCcw==
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						FileUtils.moveDirectory(vaultDir.toFile(), backupVaultDir.toFile());
						Files.createDirectories(vaultDir);
					}
					addVault(vaultDir, true);
				} catch (IOException e) {
					LOG.error("Error creating vault", e);
					err = true;
				}
			} else {
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19r3pg08IeyaPX9GMkMPUmsyrHBi8SEGgHsuxrJXn5PwqoX8D3fSjIa
FqHEV/q5o7/gsTvzGhL20uxR+oWJtuzXZJTRSHHiCw7J9ShxfoCgprTRlcvp4OW6
KNRqiSaf+Rw/WASADUA03po9rZNHE3mzgk6kel8Hfljs/zOc7rhGhS6zxmRIFvUP
RYdzusvGw0ZCmyqK7Enyry5qVk6XLwmwtURlxnQqdFjloDXNy4Lay7FllcdVwT0N
lyoWUBUKmyCteuczhG5goA==
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				LOG.warn("Vault state is NEW but vault list in settings file is not empty. Removing all stale vaults and recreate new vault.");
				while (!vaults.isEmpty()) {
					vaultList.getSelectionModel().select(vaults.get(0));
					vaults.remove(selectedVault.get());
				}
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+JL32YTqZ3eXv86+JVvpDDs7fiOKrUppZF0SXA1SEwPf6rIFlP2shx
CBgGaPpUqIQIxZUkOx2Xh3zgO1NsUkge0P03Vy3AeBqiNBBxTGDV/X7lJOBWTgaw
LvH4naq8c5HgrEyoP1601bAQ8GRVnYlDclZkmgxXifUtQOADD7c68BZAX251rgtL
yY2BP4K2R0AxR0GP1BPEy44/tPMk/FnAjjpJqqjdsLMfCvxd34COm5SXom+63YSG
Y0ky9q+JXzyprC6IhuqsHIfcvGNt11mmALnNVVZ8KzIz6SsvRz+/jN7ks2ZgJWCb
+d/kshob27k3qw92KAluzrtv0z4O99hzh/BXGtSLSQypqgIKHxJLr5xETFT446pn
vLsf7HLld+VWJBmpqffWPw==
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+4vzLw+hAmU5Gp9hkRTmeWWNa9yBMJCS46pMPQhFBLM4wE7HmBbpzY
q/SLL3Ywq/qkTWyktG1jCnjl/Ak0F+zAM7gig0zPt9VSgYYBWGNpNwGiTgocDrmw
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				try {
					final Path vaultDir = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE");
					String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
					final Path backupVaultDir = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC_conflict" + System.getProperty("file.separator") + timestamp + System.getProperty("file.separator"));
					if (!Files.exists(vaultDir)) {
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/dcLGwi3ZrLXoL7j/uez1XEc+0QbDD1M54Ie/qQxYOjj5sGGRSaUmv
VZWUhzx/lNWZ18MOH8Yl2MFFPDRt0ODVBAzNE6wLlowmZRjSo/+O5gU3vjhTmEUJ
ix/Ih9ks7l+bxmQyCRn6mkZxXE4cb8XYrVaVCMl1BoTIj/Sb/6N80Kg2Lmjbnw8l
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						Files.createDirectories(vaultDir);
					} else {
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18ExQcDv0iO3tj9BzdEEuYbPlCuxSCEZzAmlBMzZXko+GQ+riDpo9Bd
c7l3oStvXb38X+C7t+qKc2NeaVGArPr/GBxTgWTiLXxqrQbiQ/MRh+fY7JHUZ64Y
6KD/ly/4mCdrjskBbtKevA==
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						FileUtils.moveDirectory(vaultDir.toFile(), backupVaultDir.toFile());
						Files.createDirectories(vaultDir);
					}
					addVault(vaultDir, true);
				} catch (IOException e) {
					LOG.error("Error creating vault", e);
					err = true;
				}
			}
			if (err) {
				Platform.runLater(() -> {
					Alert debugInfoDialog = DialogBuilderUtil.buildInformationDialog("Error", null, "Error creating vault.", ButtonType.OK);
					debugInfoDialog.showAndWait();
				});
			}
			break;
        case RUN:
        	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+y+CXJ+j1B0GsVnY1+aTJwQeOovGT+gWDejpUz18KkPrYZYFyTfRWh
Y7vRUTsPHWVXqrh5vLrDt3UZK+FRiSEgiJv0hykDuttg/ABFWB1jnguac77xhghK
ncplTu/naE+pfEyjFan2VQ==
        	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
        	if (vaults.isEmpty()) {
        		LOG.warn("Vault state is RUN but no existing vault in settings file. Adding back vault if vault directory is present.");
				try {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18UugjWifLbHWPIAEboH3IE5zHSXoSqApSkg5Rb174YZ+pFTw04NUV3
xup4v5SZMRorcCn6Xtc3A6lvfm/EfK2v7qFJSfyhVPs=
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					final Path vaultDir = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE");
					if (Files.exists(vaultDir)) {
						addVault(vaultDir, true);
					}
					else {
						LOG.error("Existing vault directory is not present.");
						err = true;
					}
				} catch (Exception e) {
					LOG.error("Error adding vault directory.", e);
					err = true;
				}
			} else {
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX192RodpdGahihEY64Dwc08OETPqhAEeWl9c+TgIu192JRSwIqWTqfP+
9D8dO9dxed4kI8vdEjfGormYDjuzf0XJclXK8Km44k1rWSpjkzKX/EiiwKfjAHwc
rr1J/7thQRLkpPMrOHYthyHAqTOvz63L5BEWydAkSxU=
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				LOG.info("Vault state is RUN. To unlock existing vault.");

				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+PmnwoAzPmZxPDvhQMib5zir/Z+g6z+eO9uAnFE4ulGVr335n19lm9
NDcIXUMLbHXupIjb5fnM6Q==
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				LOG.debug("Checking vaultid.virtualsafe");
        		String vaultid = new String("");
				try {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/gCQ3DvRczy3rwc3j8hho+7dOUQlzjMIS92f13ZEn2N8OJXmhVC4qh
IahEQbMEmnvW/L8msZNLo3EON37o2ipcR9OJ5jEr/OM=
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					final Path vaultidfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + VAULTID_FILENAME);
	            	vaultid = new String(Files.readAllBytes(vaultidfile), StandardCharsets.US_ASCII).trim();
				} catch (Exception ex) {
					LOG.warn("Unable to load vault ID.");
				}
				if (vaultid.isEmpty()) {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18CA57O9NKknyAb9rPBV8uTcPos7yIPS6TqzTuKismJ1SPWrh5sEnBv
HYlgLhh+xGrftml4E9iaygLPCDjbGoqHLkXLH+wzpjNeZ8PhgVSM09XNNIhhWJhR
hBcUaU4X1jNEqSqYChfaRPv6lCmsW7Y2vjVN19+WsmmXIMMq4dfi8VpUQ4SZiUWT
gJMfOfjv67RHsug7GiFOmWcy8v/VwlfD7vCwxIZ1rPU=
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					try {
						LOG.debug("Saving vault ID into VAULTID_FILENAME.");
						final Path vaultidfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + VAULTID_FILENAME);
		            	Files.write(vaultidfile, vaults.get(0).getId().getBytes(StandardCharsets.US_ASCII), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
					} catch (Exception ex) {
						LOG.warn("Unable to save vault ID.");
					}
				} else {
					if (!vaultid.equals(vaults.get(0).getId())) {
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19iDRUJA4gkh5gN88v4A54JPjy6iEJlXtteOaFFPR2ARbHZHntoAf+S
kke2oxiCtQtoGye5/LX2dE3Z4cIlCUzO0jSXWqae7Km860IyzIfo7cQSNd/h+Fco
v0v+E1LylQ8JM1IwnrjJiKO+MMbV1NzKOC9f3uMwEUpNrqLdebh/zPq/MHc1f4W9
Tx1O5Lyw3izzl2rfFd3DFb+qwf0fqxn5UkB728KCPJRhUdGc9u0RCMFw0TOXo1oe
wsWU4NWYp7aTnLjkeDq0rL8Zs3Nabka+y3rQN5APT/s=
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						LOG.warn("Vault state is RUN but vault ID from settings differs from VAULTID_FILENAME. Removing existing vault from settings.");
						while (!vaults.isEmpty()) {
							vaultList.getSelectionModel().select(vaults.get(0));
							vaults.remove(selectedVault.get());
						}
						didWelcome(vstate);
					}
				}

				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19gx2BXqqVfjIa5lW4Zqo6CLjjs1S7dEu53o+S4ZSiNfeJZ/e4c5W0h
DMpszol4lweMoRV93eUjnGCvVzDcmCCan3OeHXx1poRzd5Z9LIUkoWP0F+7Vp978
EwKDk3GpXSfxsrvprKoKGq1l1A9li+PyJ2nkkDpxslw=
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				vaultList.getSelectionModel().select(vaults.get(0));
			}
			if (err) {
				Platform.runLater(() -> {
					Alert debugInfoDialog = DialogBuilderUtil.buildInformationDialog("Error", null, "Error adding existing vault directory.", ButtonType.OK);
					debugInfoDialog.showAndWait();
					this.gracefulShutdown();
				});
			}
			break;
        default:
        	LOG.error("Error checking vault state file. Shutting down.");
			try {
			    Thread.sleep(10000);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			this.gracefulShutdown();
			break;
		}
	}

	private void showPreferencesView() {
		activeController.set(viewControllerLoader.load("/fxml/settings.fxml"));
	}

	private void showNotFoundView() {
		activeController.set(viewControllerLoader.load("/fxml/notfound.fxml"));
	}

	private void showInitializeView() {
		final InitializeController ctrl = viewControllerLoader.load("/fxml/initialize.fxml");
		ctrl.setVault(selectedVault.get());
		ctrl.setListener(this::didInitialize);
		activeController.set(ctrl);
	}

	public void didInitialize() {
		showUnlockView(UnlockController.State.INITIALIZED);
		activeController.get().focus();
	}

	private void showUpgradeView() {
		final UpgradeController ctrl = viewControllerLoader.load("/fxml/upgrade.fxml");
		ctrl.setVault(selectedVault.get());
		ctrl.setListener(this::didUpgrade);
		activeController.set(ctrl);
	}

	public void didUpgrade() {
		showUnlockView(UnlockController.State.UPGRADED);
		activeController.get().focus();
	}

	private void showUnlockView(UnlockController.State state) {
		final UnlockController ctrl = viewControllerLoader.load("/fxml/unlock.fxml");
		ctrl.setVault(selectedVault.get(), state);
		ctrl.setListener(this::didUnlock);
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+pUtCO/8T3+8Bx4JENUDIwTV7XZAbR//X9wXf80VhBPiEbqODUI45O
hr84VHbLq81zsxJ3yViDDgv5eKazEw6ry+9oxGHHesSRYUpBB2IoDqR8ran1xlb6
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		ctrl.vaultState(vstate);
		activeController.set(ctrl);
	}

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/kWFOZMsbMcBKK3ezLLcjIN9FK2rz70HP8czZRPpo1e5tUmv06dRaS
qdSnQA7B6vGF6ybdcoOTx1J5NXVhvLWsQogkxBqe1w3oJws3+DOoi7GHGWvzDGi+
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private void showUnlockView(UnlockController.State state, Vault vault) {
		final UnlockController ctrl = viewControllerLoader.load("/fxml/unlock.fxml");
		ctrl.setVault(vault, state);
		ctrl.setListener(this::didUnlock);
		activeController.set(ctrl);
	}

	public void didUnlock(Vault vault) {
		if (vault.equals(selectedVault.getValue())) {
			this.showUnlockedView(vault, vault.getVaultSettings().revealAfterMount().getValue());
		}
	}

	private void showUnlockedView(Vault vault, boolean reveal) {
		final UnlockedController ctrl = unlockedVaults.computeIfAbsent(vault, k -> viewControllerLoader.load("/fxml/unlocked.fxml"));
		ctrl.setVault(vault);
		ctrl.setListener(this::didLock);
		if (reveal) {
			ctrl.revealVault(vault);
		}
		activeController.set(ctrl);
	}

	public void didLock(UnlockedController ctrl) {
		unlockedVaults.remove(ctrl.getVault());
		if (ctrl.getVault().getId() == selectedVault.get().getId()) {
			showUnlockView(UnlockController.State.UNLOCKING);
		}
		activeController.get().focus();
	}

	private void showChangePasswordView() {
		final ChangePasswordController ctrl = viewControllerLoader.load("/fxml/change_password.fxml");
		ctrl.setVault(selectedVault.get());
		ctrl.setListener(this::didChangePassword);
		activeController.set(ctrl);
		Platform.runLater(ctrl::focus);
	}

	public void didChangePassword() {
		showUnlockView(UnlockController.State.PASSWORD_CHANGED);
		activeController.get().focus();
	}

}
