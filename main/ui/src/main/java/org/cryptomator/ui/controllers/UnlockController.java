/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.WindowsDriveLetters;
import org.cryptomator.ui.util.DialogBuilderUtil;
import org.cryptomator.ui.util.Tasks;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+4NehY5RokeVu6Wuzn6KhIG9bjGAtH74jxzrQzA4GEK6FHS+/Uinvl
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
import java.security.SecureRandom;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.cryptomator.ui.util.SecureCharSequence;
import org.cryptomator.ui.util.VaultState;
import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import org.cryptomator.keychain.KeychainAccess;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import org.cryptomator.cryptolib.Cryptors;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.KeyFile;
import org.cryptomator.cryptolib.DecryptingReadableByteChannel;
import org.cryptomator.cryptolib.EncryptingWritableByteChannel;
import org.apache.commons.lang3.StringUtils;
import static org.cryptomator.ui.util.Constants.MASTERPASS_FILENAME;
import static org.cryptomator.ui.util.Constants.TOKEN_FILENAME;
import static org.cryptomator.ui.util.Constants.TOKENKEY_FILENAME;
import static org.cryptomator.ui.util.Constants.RESTORE_FILENAME;
import org.cryptomator.ui.util.PKI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger; 

public class UnlockController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);
	private static final CharMatcher ALPHA_NUMERIC_MATCHER = CharMatcher.inRange('a', 'z') //
			.or(CharMatcher.inRange('A', 'Z')) //
			.or(CharMatcher.inRange('0', '9')) //
			.or(CharMatcher.is('_')) //
			.precomputed();

	private final Application app;
	private final Stage mainWindow;
	private final Localization localization;
	private final WindowsDriveLetters driveLetters;
	private final ChangeListener<Character> driveLetterChangeListener = this::winDriveLetterDidChange;
	private final Optional<KeychainAccess> keychainAccess;
	private final Settings settings;
	private final ExecutorService executor;
	private Vault vault;
	private Optional<UnlockListener> listener = Optional.empty();
	private Subscription vaultSubs = Subscription.EMPTY;
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19UgI53cSrrfmz/okxKhc6VEEwNJxwRi2EPM5J0eSqs2ixRS/TuYsqY
smjEBTyKo7Em+0rLaf+s7g==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private VaultState.State vstate;

	@Inject
	public UnlockController(Application app, @Named("mainWindow") Stage mainWindow, Localization localization, WindowsDriveLetters driveLetters, Optional<KeychainAccess> keychainAccess, Settings settings, ExecutorService executor) {
		this.app = app;
		this.mainWindow = mainWindow;
		this.localization = localization;
		this.driveLetters = driveLetters;
		this.keychainAccess = keychainAccess;
		this.settings = settings;
		this.executor = executor;
	}

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private Button advancedOptionsButton;

	@FXML
	private Button unlockButton;

	@FXML
	private Text messageText;

	@FXML
	private CheckBox savePassword;

	@FXML
	private TextField mountName;

	@FXML
	private CheckBox revealAfterMount;

	@FXML
	private Label winDriveLetterLabel;

	@FXML
	private ChoiceBox<Character> winDriveLetter;

	@FXML
	private CheckBox useCustomMountPoint;

	@FXML
	private HBox customMountPoint;

	@FXML
	private Label customMountPointLabel;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Text progressText;

	@FXML
	private Hyperlink downloadsPageLink;

	@FXML
	private GridPane advancedOptions;

	@FXML
	private GridPane root;

	@FXML
	private CheckBox unlockAfterStartup;

	@FXML
	private CheckBox useReadOnlyMode;

	@Override
	public void initialize() {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18M8y11fcjCVB0QXb8lO/a9nVnpgcI6i4u4zj2YaXoiqbm2ZQtx+m5Y
MoxJrl5AoDdmY8Bf+VLTPQ==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		advancedOptionsButton.setVisible(false);
		savePassword.setVisible(false);

		advancedOptions.managedProperty().bind(advancedOptions.visibleProperty());
		unlockButton.disableProperty().bind(passwordField.textProperty().isEmpty());
		mountName.addEventFilter(KeyEvent.KEY_TYPED, this::filterAlphanumericKeyEvents);
		mountName.textProperty().addListener(this::mountNameDidChange);
		savePassword.setDisable(!keychainAccess.isPresent());
		unlockAfterStartup.disableProperty().bind(savePassword.disabledProperty().or(savePassword.selectedProperty().not()));

		customMountPoint.visibleProperty().bind(useCustomMountPoint.selectedProperty());
		customMountPoint.managedProperty().bind(useCustomMountPoint.selectedProperty());
		winDriveLetter.setConverter(new WinDriveLetterLabelConverter());

		if (!SystemUtils.IS_OS_WINDOWS) {
			winDriveLetterLabel.setVisible(false);
			winDriveLetterLabel.setManaged(false);
			winDriveLetter.setVisible(false);
			winDriveLetter.setManaged(false);
		}

		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+Pt9OM/xrg9O05KRTQbdHLuydyI4d7zEBFAsEDnzoAdPBpFw+518Gj
cJdee39sb6oGiRCKDdVJaSH+0fxxNd4ORkUZ8hW2DHZ2g3uVnfX8eDUkkmEgH4Bn
BgPkZjWcot1OeHnFRi8Oi6EMMBN3pGOtR0sHUoQujuT1fclcLOfSBz20k+0AsYFb
mFlpVZ9JsFRwo9UBtT25goagdGYXhkEIOIbpIXfGA7QsglpfWvA2cVgpkifC5DJF
j2ORPyb3kvarnzWbsEZyiw7PxfpmyJVGaabmhNSL1pIoXVIfBeIjlCOADlTCsOVA
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
    	Platform.runLater(() -> {
			settings.preferredVolumeImpl().set(VolumeImpl.WEBDAV);
		});
	}


	@Override
	public Parent getRoot() {
		return root;
	}

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19IymVmsdb/IpPr6etQjNwImEwx63LbsgW+Xnmox8N3/1yhPqHKd0E2
u40Y5QuDAIBIMHVgpiN3qLjtBz5aWOWymaUoGzs6iyFnHNJx11YclIqLBxrUViNo
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	void vaultState(VaultState.State vs) {
		this.vstate = vs;
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19tPVqCu6Cq4a6gTKud2B6bj/1Xynhi9SIC99xPS64vd2SVEsEzVJLH
stlymIk1p9U8ONwYXhHyqA==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		LOG.debug("vault state set to {}", vs);
	}

	@Override
	public void focus() {
		passwordField.requestFocus();
	}

	void setVault(Vault vault, State state) {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX195E2lEF7/cSe8DNXBnWRUErswWrvd9PgdulZdTfduY3MsgA/Yv7fNm
aQwGn6w9ASJArWR+IxzB1Q==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		LOG.debug("setVault to {} with state {}", vault.getMountName(), state);
		vaultSubs.unsubscribe();
		vaultSubs = Subscription.EMPTY;

		// trigger "default" change to refresh key bindings:
		unlockButton.setDefaultButton(false);
		unlockButton.setDefaultButton(true);
		if (Objects.equals(this.vault, Objects.requireNonNull(vault))) {
			return;
		}
		assert vault != null;
		this.vault = vault;
		advancedOptions.setVisible(false);
		advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.show"));
		progressIndicator.setVisible(false);
		progressText.setText(null);
		state.successMessage().map(localization::getString).ifPresent(messageText::setText);
		if (SystemUtils.IS_OS_WINDOWS) {
			winDriveLetter.valueProperty().removeListener(driveLetterChangeListener);
			winDriveLetter.getItems().clear();
			winDriveLetter.getItems().add(null);
			winDriveLetter.getItems().addAll(driveLetters.getAvailableDriveLetters());
			winDriveLetter.getItems().sort(new WinDriveLetterComparator());
			winDriveLetter.valueProperty().addListener(driveLetterChangeListener);
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19cfxe0mDpJQpbtUGV/gGQow1sQ+/B4ufa/usERLZqHuCeiMB1Z4QJk
nrdAIK8kj0rggvQ2YTzAbDbulmQv8yMLi92qIaQf3gRUYHJ8DL1jCJV6eo4yB2WQ
p4hewYzFB1DvcxtzlwJjrAk8gSHZeiOwgYcybDAoIz2+zc1JOn5+xSNhB++f5fpd
sujuMlTTI4/IN194Ft2qr0//jeE8F6ljTB6QDN2Jc3hU+b+j2FU959WC4Bx9ZMIA
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			vault.setWinDriveLetter('V');
			chooseSelectedDriveLetter();
		}
		downloadsPageLink.setVisible(false);
		mountName.setText(vault.getMountName());
		savePassword.setSelected(false);
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18yMsopi230Hwd3XkVfsYtI+ewtgXfsD+LcWKKkbnIHC0SlxyCjI9zF
uNTnWPZzus8r/aCL7XglFCBUtmIXh0Hjk2DAxZPDxsajwwrBwJoQ6QfA1+Z36QzD
7QyXKFETPPl6h7l87B5asMb4q7q/1umZVhaBCzvQdCxC1HQ6ClBU6JdDN5H0xQ+U
cVhZ1B1jp4bP8LbnbkePBz3yZPIyFA56W1PEdQv0Mt7zSIJYVY9abNRA/u9dG9YR
XOW+yoDQlprR+NqfXRgBBsHrbI3agtE5FFfPCZaPLUZBKJ5pOi9LXg3+KW8FZvVB
Dfwa9GLbTHkrJ2RBL0tAEpGk26M2Dn8T6OGude0KWF4T9Z3O+fazCrDjj4vH8QhR
L24d6m/8EPzro9lNPXDej1sbDwfFs4hh0KMjyGbjR/XdA/Wti7G60wtD7zXAw1x7
yPOFdxK0LAv+GCSFiquxd7L+WGqBFzZLADSYI+Urt4Aah/60jSF5Kt2LNTvJXU30
CmdDVSBxLUT2HVflTQFWKk/Rkhuol5JLvGnHRr7IdhwBuCsEz+osysBoWcB8p9Nv
R/dLOMaTixv9GC3WibVPoA==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		VaultSettings vaultSettings = vault.getVaultSettings();
		unlockAfterStartup.setSelected(savePassword.isSelected() && vaultSettings.unlockAfterStartup().get());
		revealAfterMount.setSelected(vaultSettings.revealAfterMount().get());

		// WEBDAV-dependent controls:
		if (VolumeImpl.WEBDAV.equals(settings.preferredVolumeImpl().get())) {
			useCustomMountPoint.setVisible(false);
			useCustomMountPoint.setManaged(false);
		} else {
			useCustomMountPoint.setVisible(true);
			useCustomMountPoint.setSelected(vaultSettings.usesIndividualMountPath().get());
			if (Strings.isNullOrEmpty(vaultSettings.individualMountPath().get())) {
				customMountPointLabel.setText(localization.getString("unlock.label.chooseMountPath"));
			} else {
				customMountPointLabel.setText(displayablePath(vaultSettings.individualMountPath().getValueSafe()));
			}
		}

		// DOKANY-dependent controls:
		if (VolumeImpl.DOKANY.equals(settings.preferredVolumeImpl().get())) {
			winDriveLetter.visibleProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetter.managedProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetterLabel.visibleProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetterLabel.managedProperty().bind(useCustomMountPoint.selectedProperty().not());
			// readonly not yet supported by dokany
			useReadOnlyMode.setSelected(false);
			useReadOnlyMode.setVisible(false);
			useReadOnlyMode.setManaged(false);
		} else {
			useReadOnlyMode.setSelected(vaultSettings.usesReadOnlyMode().get());
		}

		// OS-dependent controls:
		if (SystemUtils.IS_OS_WINDOWS) {
			winDriveLetter.visibleProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetter.managedProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetterLabel.visibleProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetterLabel.managedProperty().bind(useCustomMountPoint.selectedProperty().not());
		}

		vaultSubs = vaultSubs.and(EasyBind.subscribe(unlockAfterStartup.selectedProperty(), vaultSettings.unlockAfterStartup()::set));
		vaultSubs = vaultSubs.and(EasyBind.subscribe(revealAfterMount.selectedProperty(), vaultSettings.revealAfterMount()::set));
		vaultSubs = vaultSubs.and(EasyBind.subscribe(useCustomMountPoint.selectedProperty(), vaultSettings.usesIndividualMountPath()::set));
		vaultSubs = vaultSubs.and(EasyBind.subscribe(useReadOnlyMode.selectedProperty(), vaultSettings.usesReadOnlyMode()::set));
	}

	private String displayablePath(String path) {
		Path homeDir = Paths.get(SystemUtils.USER_HOME);
		Path p = Paths.get(path);
		if (p.startsWith(homeDir)) {
			Path relativePath = homeDir.relativize(p);
			String homePrefix = SystemUtils.IS_OS_WINDOWS ? "~\\" : "~/";
			return homePrefix + relativePath.toString();
		} else {
			return p.toString();
		}
	}

	// ****************************************
	// Downloads link
	// ****************************************

	@FXML
	public void didClickDownloadsLink() {
		app.getHostServices().showDocument("https://cryptomator.org/downloads/#allVersions");
	}

	// ****************************************
	// Advanced options button
	// ****************************************

	@FXML
	private void didClickAdvancedOptionsButton() {
		messageText.setText(null);
		advancedOptions.setVisible(!advancedOptions.isVisible());
		if (advancedOptions.isVisible()) {
			advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.hide"));
		} else {
			advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.show"));
		}
	}

	private void filterAlphanumericKeyEvents(KeyEvent t) {
		if (!Strings.isNullOrEmpty(t.getCharacter()) && !ALPHA_NUMERIC_MATCHER.matchesAllOf(t.getCharacter())) {
			t.consume();
		}
	}

	private void mountNameDidChange(@SuppressWarnings("unused") ObservableValue<? extends String> property, @SuppressWarnings("unused")String oldValue, String newValue) {
		// newValue is guaranteed to be a-z0-9_, see #filterAlphanumericKeyEvents
		if (newValue.isEmpty()) {
			mountName.setText(vault.getMountName());
		} else {
			vault.setMountName(newValue);
		}
	}

	@FXML
	public void didClickChooseCustomMountPoint() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		File file = dirChooser.showDialog(mainWindow);
		if (file != null) {
			customMountPointLabel.setText(displayablePath(file.toString()));
			vault.setCustomMountPath(file.toString());
		}
	}

	/**
	 * Converts 'C' to "C:" to translate between model and GUI.
	 */
	private class WinDriveLetterLabelConverter extends StringConverter<Character> {

		@Override
		public String toString(Character letter) {
			if (letter == null) {
				return localization.getString("unlock.choicebox.winDriveLetter.auto");
			} else {
				return letter + ":";
			}
		}

		@Override
		public Character fromString(String string) {
			if (localization.getString("unlock.choicebox.winDriveLetter.auto").equals(string)) {
				return null;
			} else {
				return CharUtils.toCharacterObject(string);
			}
		}

	}

	/**
	 * Natural sorting of ASCII letters, but <code>null</code> always on first, as this is "auto-assign".
	 */
	private static class WinDriveLetterComparator implements Comparator<Character> {

		@Override
		public int compare(Character c1, Character c2) {
			if (c1 == null) {
				return -1;
			} else if (c2 == null) {
				return 1;
			} else {
				return c1 - c2;
			}
		}
	}

	private void winDriveLetterDidChange(@SuppressWarnings("unused") ObservableValue<? extends Character> property, @SuppressWarnings("unused") Character oldValue, Character newValue) {
		vault.setWinDriveLetter(newValue);
	}

	private void chooseSelectedDriveLetter() {
		assert SystemUtils.IS_OS_WINDOWS;
		// if the vault prefers a drive letter, that is currently occupied, this is our last chance to reset this:
		if (driveLetters.getOccupiedDriveLetters().contains(vault.getWinDriveLetter())) {
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18MRtsGq2q/Rt0dn7U6vbpBGO7fIjzolpc0u8b2jSJsQf6DEbO9LKvq
b2TCGeUZEVl0q4zHIRGfCte0LSCoNVufTz7fG9VWoOriWlTIMqXbyJm3cx+2jddk
0L/UK4bBC3qA5DQS8SR+4Z4xD4NsrVHiCZ5qqXcOsVmI3YOqOAiFo8LePEbHaPbs
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			vault.setWinDriveLetter(driveLetters.getAvailableDriveLetters().iterator().next());
			LOG.debug("winDriveLetter occupied, setting to {}",vault.getWinDriveLetter());
		}
		Character letter = vault.getWinDriveLetter();
		if (letter == null) {
			// first option is known to be 'auto-assign' due to #WinDriveLetterComparator.
			this.winDriveLetter.getSelectionModel().selectFirst();
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+tej1sasUsMS1lOs8n8YTPGvDi3PYE0jDJ7E65N+Rxv6RSwXNT5xbD
7EctvEVrWv29s1vGPqJQM8QId4agPaH92Xx5lfALk2P/pT7jgp5pPc0Ky5o08ZoM
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			letter = this.winDriveLetter.getValue();
			vault.setWinDriveLetter(letter);
		} else {
			this.winDriveLetter.getSelectionModel().select(letter);
		}
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18Jaiqev3vusnSrcCMMcoUJUIMUA9HhCuQLi3lLSxCPMUezVaeietri
qSQXdNlepY4WxZR5WyEhpw==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		LOG.debug("chooseSelectedDriveLetter() decided on {}", letter);
	}

	// ****************************************
	// Save password checkbox
	// ****************************************

	@FXML
	private void didClickSavePasswordCheckbox() {
		if (!savePassword.isSelected() && hasStoredPassword()) {
			Alert confirmDialog = DialogBuilderUtil.buildConfirmationDialog( //
					localization.getString("unlock.savePassword.delete.confirmation.title"), //
					localization.getString("unlock.savePassword.delete.confirmation.header"), //
					localization.getString("unlock.savePassword.delete.confirmation.content"), //
					SystemUtils.IS_OS_MAC_OSX ? ButtonType.CANCEL : ButtonType.OK);

			Optional<ButtonType> choice = confirmDialog.showAndWait();
			if (ButtonType.OK.equals(choice.get())) {
				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19W+FWEkIQ2dkEoXQk/fySw5mABVk+7V1v8IMBN6eKuwNLAeKnwXraW
9vAsulb94NEZEsvzEtd7I+FEiE6pD0FJE4MZdEkdMUk13VlxTnppB22Ki7Pv52K9
kBCl9seMbGxoKP3/y8gim1eTcT1RZudcY+tIaTmCZeM=
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				keychainAccess.get().deletePassphrase(vault.getId()+"_RANDOMPASS");
			} else if (ButtonType.CANCEL.equals(choice.get())) {
				savePassword.setSelected(true);
			}
		}
	}

	private boolean hasStoredPassword() {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+eht3J85gR3lxTZhvRP3PrxgXiioJ9zZBngUIfOahbM7KHM5vGcCoA
dnSC6eK4HyIg2j/E0nZNkNsxSdHw6oD8EXWnmE/sPZiLMhUY1WTMQfZSJ1b9psSH
0MHLP21s+JaUkj8Z3DjV/Rf2u6ufHXY+8gZcqI14cKI=
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		char[] storedPw = keychainAccess.get().loadPassphrase(vault.getId()+"_RANDOMPASS");
		boolean hasPw = (storedPw != null);
		if (storedPw != null) {
			Arrays.fill(storedPw, ' ');
		}
		return hasPw;
	}

	// ****************************************
	// Unlock button
	// ****************************************

	@FXML
	private void didClickUnlockButton() {
		advancedOptions.setDisable(true);
		advancedOptions.setVisible(false);
		advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.show"));
		progressIndicator.setVisible(true);

		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
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
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */

		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19RHLGkMWqPUUCvEPDT/mRblvv8QC0utPa5kXn9rvQBTyWkbAvTaxta
a+lxWFnURCrH0KPU/czD8w==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		if (!keychainAccess.isPresent()) {
			LOG.error("Error accessing system keychain.");
			messageText.setText(localization.getString("additional.errorMessage.cannotAccessSystemKeychain"));
			return;
		} else {
			final SecureCharSequence storedRandomPw = new SecureCharSequence(keychainAccess.get().loadPassphrase(vault.getId()+"_RANDOMPASS"));
			if (StringUtils.isBlank(storedRandomPw)) {
				if (vstate == VaultState.State.NEW) {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18HFIAb5FOOIYdqPW/IdEq0zuA3+xboTJ64Ezxb4sxILNDEaDru3Jq1
7i16sm7GkJkEBCEAQ4AulP8KwGz4Mc+HmGHTmRymf3dWXQ57huCOytjRv3v6JC6q
Kt+SVaeTmWnYePnJxHwR0g==
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					LOG.debug("Vault state is NEW and RANDOMPASS is not present on keychain.");
					LOG.error("Unable to unlock vault. Vault state is NEW and RANDOMPASS is not present on keychain.");
					messageText.setText(localization.getString("additional.errorMessage.vaultStateNEWbutCannotAccessRANDOMPASS"));
					return;
				} else if (vstate == VaultState.State.RUN) {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+UM4VSbIRQ1hgCtI0NjPY87AsVE7sMQJEoPbGqTz9LBzQfuw1qFZwt
YFSeh9fLvn/MOhWqaAcfMveNek0kc+R6N8mpXRtzq1CD0WXZooOvnmF47Sk8tzgC
T2EsZ+J7rwNVxC9Ftlw6hA==
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					LOG.debug("Vault state is RUN and RANDOMPASS is not present on keychain.");
					final Path tokenkeyfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + TOKENKEY_FILENAME);
					final Path restorefile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + RESTORE_FILENAME);
					if (Files.exists(tokenkeyfile) && Files.exists(restorefile)) {
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/ZTF/81CHwIxOEqpX2H1TsM/8B98Rvhok2UbhYH1VaXB6fYavedJiM
hGmEtrc7/hM2bWb5LEWhM0lntUD7fAUjS/AUf/vRjggJ8n3+0roK06UOjAWmsnp9
1aov9btdqjT+oCsg30Qw/myqUzR1lv+6ruk8lNExFOCOV/O/teFv2RKBwJu2OCAR
9h9mdaAOcH5gznHxBJd8jyFUEQsNRjyMUWrUssqNBGyGOUTlm+Zdk38L8U53K5Kk
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						LOG.debug("Token keyfile & encrypted RANDOMPASS restorefile are present. Attempting to restore.");

						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+JXo2dSWD+B6GVkRrVvJ9DcATspEf1GVr1fZbfr4tY/mlBhVnuHWtt
doeSgrgN0V78HObcnByxfg==
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						Dialog dialog = new TextInputDialog();
						dialog.setTitle(localization.getString("additional.errorMessage.RANDOMPASSnotPresent"));
						dialog.setHeaderText(localization.getString("additional.message.requestToken"));
						Optional<String> result = dialog.showAndWait();
						String input = "";
						if (result.isPresent()) {
							input = result.get();
						} else {
							progressIndicator.setVisible(false);
							return;
						}
						final String inputToken = input;

						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/dSPjPA8U3nbr6gpSxFTadWvl8IQp1/sd4+fokHCke4bNsN1X1aIY8
Klobg0RXi8n/8dZaNe7xXI/1uJV7HOSx99P/LwooDED2tw7H51rq9qodJdoe/9dW
i3fITm5aK7yT2UzztVxdNN+a2vhkquk/KixG0CfgufH4wnj7iCo5g89UWoKPnDJT
pTkghbo1HgblNE29dN3ckQkBzFEQbdJEuJH43vCWsLSo2+o+lRGJFQmSmWLlVgL/
pquE0YLuBNmod2Osz6IGmqPhVRCcQYf9DPudluSYixBgevQm4+WfnQwBdGYMJGNf
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						ByteBuffer plaintext = ByteBuffer.allocate(50);
						try {
							byte[] tokenkeyFileContents = Files.readAllBytes(tokenkeyfile);	
							KeyFile keyFile = KeyFile.parse(tokenkeyFileContents);
							final SecureRandom random = new SecureRandom();
							CryptorProvider provider = Cryptors.version1(random);
							Cryptor cryptor = provider.createFromKeyFile(keyFile, inputToken, 42);

					        
					        ReadableByteChannel ciphertextIn = Files.newByteChannel(restorefile);
					        try (ReadableByteChannel ch = new DecryptingReadableByteChannel(ciphertextIn, cryptor, true)) {
								ch.read(plaintext);
					        }
					        plaintext.flip();
						} catch (InvalidPassphraseException e) {
							messageText.setText(localization.getString("additional.errorMessage.invalidToken"));
							LOG.error("Restoration Token is invalid.");
							progressIndicator.setVisible(false);
							return;
						} catch (UncheckedIOException | IOException ex) {
							messageText.setText(localization.getString("additional.errorMessage.cannotDecryptToken"));
							LOG.error("Failed to decrypt of Restoration Token.");
							progressIndicator.setVisible(false);
							return;
						} catch (Exception ex) {
							LOG.error("Unable to restore RANDOMPASS with restoration token.", ex);
							progressIndicator.setVisible(false);
							return;
						}
				        /* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19EpUUbeqRyW6X0/XdLzmH6JfBWHpem/G+mCJN3B4X3Ie8kLWJijgkr
84R6ITJFWLfuD/Wy/N12peyE6I+eNL6RFkktWaDunk7c9Ark9Ze52cympUEthAuZ
+Ey8B7LGVy3hLp02tTBoBw==
				        ###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				        final SecureCharSequence decryptedRandomPw = new SecureCharSequence(Charset.defaultCharset().decode(plaintext));
				        LOG.info("Restoration Token is valid. Attempting to unlock vault.");

				        /* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/lkcVtK1wWbJ/0c1Ww+ubYEo97XBK84yFBoOdSEH1W/RConEeGvEyT
XZ+MGoJEnvZ8SQHv+zNcgXM7G/rtjbC/MfN4I+ZHCO6zbqg0UFw9apo+pZcuJftR
pGLtWUFXrK9h1RH14zMks+bi99FDbtXy48p4ZvOoXblx0QzawkPCtm30p+k/VN/f
6R3qlXJOicVTugRFL1ZWJPNGMifAE+UKvKwV+SmjogqCVCPOYxJhFhTdVnUQiv0A
im0CEWPWjuyzsSnTKmjtpcF14xF4Td1ErebZsj0BhJ+TUAGQs+al1J/0+OJ3DrKR
ybuxZG33iJqDaU1F+P3tgPzmyQBB/yPALc6hOoyoL964eZOhfAyZqk7fCqtJRiEe
				        ###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						final SecureCharSequence fullUnlockPw = new SecureCharSequence(passwordField.getCharacters() + decryptedRandomPw.toString());
						Tasks.create(() -> {
							progressText.setText(localization.getString("unlock.pendingMessage.unlocking"));
							vault.unlock(fullUnlockPw);
						}).onSuccess(() -> {
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/o0D4CDroe8yS2TJu6mxunA1R32ur55LDQq3V5fYj+P8SWKoY2vW13
gzMV8vXINHpiSOYACGyrIw==
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							LOG.debug("Vault unlocked with recovered RANDOMPASS & TOKEN. Storing both in keychain.");
							if (keychainAccess.isPresent()) {
								keychainAccess.get().storePassphrase(vault.getId()+"_RANDOMPASS", decryptedRandomPw.toString());
								keychainAccess.get().storePassphrase(vault.getId()+"_TOKEN", inputToken);
							}
							decryptedRandomPw.wipe();
							messageText.setText("");
							downloadsPageLink.setVisible(false);
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/y5sLisrA39yzL/ewqZvWgN05d598wsOKIG40f2mHDca+EtX/qGOjh
N6a62QAVJZpy4dNoGYa4dsJZuwW3OIvUZk5TsNvpoRke06OIo8odYhjrOjaGXo99
SVOdBJVPgOaXGIzX6id9BVWi1y9B6Web13SLp0hcAPMVWNFXt1/hoaNBtZkUm7ke
ktTYLzLjn9WqrFyId3qNFtlkbkdf5yHBPErMiF0znn54dsHCq9H2wFuceB4yEOke
sVf6WAfaACsU5YZpocSomSITxkqBR3YeoPa968tN7R7iHX8kNtJQyXGfcfb7UIAf
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							final PKI pki = new PKI();
							final Path passwordbackupfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + MASTERPASS_FILENAME);
							final Path tokenbackupfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + TOKEN_FILENAME);
							final String pubkeyfile;
							if (SystemUtils.IS_OS_WINDOWS) {
								pubkeyfile = System.getProperty("user.home")
								+ System.getProperty("file.separator") + "AppData"
								+ System.getProperty("file.separator") + "Roaming"
								+ System.getProperty("file.separator") + "VirtualSAFE"
								+ System.getProperty("file.separator") + "public_key.der";
							} else {
								pubkeyfile = System.getProperty("user.home")
								+ System.getProperty("file.separator") + ".VirtualSAFE"
								+ System.getProperty("file.separator") + "public_key.der";						
							}
							try {
								if (keychainAccess.isPresent()) {
									String pubkeyChecksum = "";
									SecureCharSequence storedPubkeyChecksum = new SecureCharSequence(keychainAccess.get().loadPassphrase(vault.getId()+"_PUBKEY"));
									try {
										MessageDigest md = MessageDigest.getInstance("MD5");
										md.update(Files.readAllBytes(Paths.get(pubkeyfile)));
										byte[] digest = md.digest();
										BigInteger no = new BigInteger(1, digest);
										pubkeyChecksum = no.toString(16); 
							            while (pubkeyChecksum.length() < 32) { 
							                pubkeyChecksum = "0" + pubkeyChecksum; 
							            }
							            pubkeyChecksum = pubkeyChecksum.toUpperCase();
									} catch (NoSuchAlgorithmException e) {
										throw new RuntimeException(e);
									}
									if (StringUtils.isBlank(storedPubkeyChecksum)) {
										keychainAccess.get().storePassphrase(vault.getId()+"_PUBKEY", pubkeyChecksum);
										pki.encryptWithEmbeddedPublicKey(pubkeyfile, fullUnlockPw, passwordbackupfile);
										pki.encryptWithEmbeddedPublicKey(pubkeyfile, new SecureCharSequence(inputToken), tokenbackupfile);
									} else {
										if (!pubkeyChecksum.equals(storedPubkeyChecksum.toString())) {
											Platform.runLater(() -> {
												Alert debugInfoDialog = DialogBuilderUtil.buildInformationDialog("Info", null, localization.getString("additional.message.pubkeyChanged"), ButtonType.OK);
												debugInfoDialog.showAndWait();
											});
											keychainAccess.get().storePassphrase(vault.getId()+"_PUBKEY", pubkeyChecksum);
											pki.encryptWithEmbeddedPublicKey(pubkeyfile, fullUnlockPw, passwordbackupfile);
											pki.encryptWithEmbeddedPublicKey(pubkeyfile, new SecureCharSequence(inputToken), tokenbackupfile);
										}
									}
								}
							} catch (Exception ex) {
								LOG.warn("Unable to backup a pubkey-encrypted copy of USERPASS+RANDOMPASS and TOKEN in the vault.");
							}
							listener.ifPresent(lstnr -> lstnr.didUnlock(vault));
							passwordField.swipe();
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+JYg6TgCVjg6p+bY1qbV9+YRAHeaKWiqJqD91P62yU5/EFQvL1N3+0
HUyu7UxRgh/8PcwaLzjtm+fsnDlInmBiJgxpTJ2ye21n/5WUnjvlLuHsYaUF9OZi
VtgiJuji+tXS+xXIXEYZgivuw1qjfxVtGcImd0qsiDu0KcWQVWyLnGMCrQfCqaze
zkb4C728zO7OEiim/azVlwotlw6iLRqC/kve2iqnP0mldSyBy7G3U3J7PVUEf//8
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							passwordField.setPassword("");
						}).onError(InvalidPassphraseException.class, e -> {
							messageText.setText(localization.getString("unlock.errorMessage.wrongPassword"));
							passwordField.selectAll();
							passwordField.requestFocus();
						}).onError(UnsupportedVaultFormatException.class, e -> {
							if (e.isVaultOlderThanSoftware()) {
								// whitespace after localized text used as separator between text and hyperlink
								messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.vaultOlderThanSoftware") + " ");
								/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/g6jVYCT8znoKbUALXdjN2dVwqSvMTffre/1S+BmXh/jATtuFKOdBn
EC2jjsrhBSqX8hOeUQWbXQ==
								###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
								//downloadsPageLink.setVisible(true);
							} else if (e.isSoftwareOlderThanVault()) {
								messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.softwareOlderThanVault") + " ");
								/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/Z5hm2YGGQyiqMZw6oM5zZFUMT/hG2kYyfsOpZn1j0SuvfD9O7k/Go
V4XkZCIGYQh8wl3v6F1uwA==
								###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
								//downloadsPageLink.setVisible(true);
							} else if (e.getDetectedVersion() == Integer.MAX_VALUE) {
								messageText.setText(localization.getString("unlock.errorMessage.unauthenticVersionMac"));
							}
						}).onError(NotDirectoryException.class, e -> {
							LOG.error("Unlock failed. Mount point not a directory: {}", e.getMessage());
							advancedOptions.setVisible(true);
							messageText.setText(null);
							showUnlockFailedErrorDialog("unlock.failedDialog.content.mountPathNonExisting");
						}).onError(DirectoryNotEmptyException.class, e -> {
							LOG.error("Unlock failed. Mount point not empty: {}", e.getMessage());
							advancedOptions.setVisible(true);
							messageText.setText(null);
							showUnlockFailedErrorDialog("unlock.failedDialog.content.mountPathNotEmpty");
						}).onError(Exception.class, e -> { // including RuntimeExceptions
							LOG.error("Unlock failed for technical reasons.", e);
							messageText.setText(localization.getString("unlock.errorMessage.unlockFailed"));
						}).andFinally(() -> {
							fullUnlockPw.wipe();
							advancedOptions.setDisable(false);
							progressIndicator.setVisible(false);
							progressText.setText(null);
						}).runOnce(executor);
					} else {
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX197djg8+h3RPTMrdxNilCne/cWREUNttX0oOHb0LwkzBaPjO27sM7yj
zAKm3aGRAEyIn/aPPXVZeGj6y4jVtSSfC8pbl+xpG0Bm21bAcYahZ8D4mhAYR65z
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						LOG.debug("Token keyfile and/or encrypted RANDOMPASS restorefile not present. Cannot proceed.");
						LOG.error("Unable to unlock vault. Vault state is RUN but unable to restore RANDOMPASS using TOKEN.");
						messageText.setText(localization.getString("additional.errorMessage.unableRecoverRANDOMPASSfromToken"));
						progressIndicator.setVisible(false);
						return;
					}
				}
			} else {
				if (vstate == VaultState.State.NEW) {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19dJc1Lj0H4cSM4ffT3jLb8fe19XoIjOYRInJI2/bRQS/9XjcFTWTQq
uHBfZ8JI+jIC8nwmh6z3S2ixkUD7tmjJkcyryrD6+6nDbtfDAM1PbuW3vi37EDYM
OzSroSoQCul1bvaZwRn7b+cWXHbxI6H4mChtpQFEnJ9bNS8084VeqjgwQSzzT9PB
4GwOUZ8WKKutloeKQNHUyk+BnsFeTaXm4cOYMDBkSmGgRZrjCOZ3QjUNCA23QnHx
C5bXDhyzuMwF3VMNViGA+fjzvSXvCWrhhpHx2NSw/7tVd3cxGw+lBXM0gQlG4M0w
uLOig7uAXtjiwuAjnb1pzTO39jKIZx6czR55BY26MiM=
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					LOG.debug("Vault state is NEW and RANDOMPASS is present on keychain. Unlock vault with USERPASS+RANDOMPASS. Set vault state to RUN.");
					final SecureCharSequence fullUnlockPw = new SecureCharSequence(passwordField.getCharacters() + storedRandomPw.toString());
					//LOG.debug("fullUnlockPw: {}", fullUnlockPw.toString());
					Tasks.create(() -> {
						progressText.setText(localization.getString("unlock.pendingMessage.unlocking"));
						vault.unlock(fullUnlockPw);
						fullUnlockPw.wipe();
					}).onSuccess(() -> {
						VaultState vs = new VaultState();
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18fU4aOsYxOCpRqGybzFBaZodeyrPe/O4txIMivEBZ3GiWH58JLf6Sc
i9VivAifOZlNAB0buA1z+paFRD9v2HY03vbdEMKf34SZnl/iyE6ILuGh3X/X/S6w
nn7mz54yhxmny6dMa1JCJA==
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						vs.setState(VaultState.State.RUN);
						messageText.setText("");
						downloadsPageLink.setVisible(false);
						listener.ifPresent(lstnr -> lstnr.didUnlock(vault));
						passwordField.swipe();
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1919wrMLHWtQCr52IiuR+5LGPYLJTN3TONzO19RCG8aWJYlaWpm60Cu
7aGs5H9IZkHros309sHd5UiJXtIR7v11nKQlZuG6v2V0WDb1dsa4Exe+j1+EmAW8
Yii4tmfHhpkmnYBFggkNYYjSeRK92VBbP2dVLYqgvuEPWLHFSKvn54L0pBC7vNBs
tK3YCuq4UhVQQGMYMbLX2IFzbnQ9T1DaxCPMuvkYsuie+81GQBmg6A3NTy6xF3OY
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						passwordField.setPassword("");
					}).onError(InvalidPassphraseException.class, e -> {
						messageText.setText(localization.getString("unlock.errorMessage.wrongPassword"));
						passwordField.selectAll();
						passwordField.requestFocus();
					}).onError(UnsupportedVaultFormatException.class, e -> {
						if (e.isVaultOlderThanSoftware()) {
							// whitespace after localized text used as separator between text and hyperlink
							messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.vaultOlderThanSoftware") + " ");
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19oxBbjsDXXTyrJt0+l/iWBe0A/dO8fk0atVLGMqQRSadi3sjXwKFs2
Xsw333FXt/EukuRTlUyC1w==
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							//downloadsPageLink.setVisible(true);
						} else if (e.isSoftwareOlderThanVault()) {
							messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.softwareOlderThanVault") + " ");
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+DCAKVxeR7vICw68P4lcyPCdXZl4eXizZfHKNUjdtDulX5w7pnlrYN
hjA9igsJgVU0aUioYhJZDA==
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							//downloadsPageLink.setVisible(true);
						} else if (e.getDetectedVersion() == Integer.MAX_VALUE) {
							messageText.setText(localization.getString("unlock.errorMessage.unauthenticVersionMac"));
						}
					}).onError(NotDirectoryException.class, e -> {
						LOG.error("Unlock failed. Mount point not a directory: {}", e.getMessage());
						advancedOptions.setVisible(true);
						messageText.setText(null);
						showUnlockFailedErrorDialog("unlock.failedDialog.content.mountPathNonExisting");
					}).onError(DirectoryNotEmptyException.class, e -> {
						LOG.error("Unlock failed. Mount point not empty: {}", e.getMessage());
						advancedOptions.setVisible(true);
						messageText.setText(null);
						showUnlockFailedErrorDialog("unlock.failedDialog.content.mountPathNotEmpty");
					}).onError(Exception.class, e -> { // including RuntimeExceptions
						LOG.error("Unlock failed for technical reasons.", e);
						messageText.setText(localization.getString("unlock.errorMessage.unlockFailed"));
					}).andFinally(() -> {
						advancedOptions.setDisable(false);
						progressIndicator.setVisible(false);
						progressText.setText(null);
					}).runOnce(executor);
				} else if (vstate == VaultState.State.RUN) {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18821UkZGEFUu5uTByFPUdHu5LGgPUcJ0TPOjjqPO8e3QicFyK1o0LL
sXz9epKCcGK/sYG+Dv92ENnIoX1TZI9tFc4urSLjcvPAeumuaK+Gsj0Dg52H0XBJ
NJkl43UZC3sohEABBXsfL5Tvg2IdqgLBiP+D8mUJLmq5NpTn1c07Ubv7ZF4jg675
Nte6tMtShQQYxFNcoTaQAhI9gW5RUnp5BYfY8DEJ/lw174z0jqiuH5w0Km/O8K+V
uZLn4nxn1iG6fsgrNuAZWUqsKP4Iqo31hlpfyoBqplIHZFmPPlTIdXcadtLWC3RW
5YwkzXo+nG779azrs0PMV6iUCjIC9pPkgG6uGJPJudS/t2fBZBpxdAPGzJWr4ItE
ly8vNJPGEIIgsEZSndGZNcg0VdqzLgBm3K/5EKkjQm32BozG/jSXJf93r5wbrabq
e+6cYwcpUKlaH2qabTeSHw==
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					LOG.debug("Vault state is RUN and RANDOMPASS is present on keychain. Unlock vault with USERPASS+RANDOMPASS.");
					final SecureCharSequence fullUnlockPw = new SecureCharSequence(passwordField.getCharacters() + storedRandomPw.toString());
					Tasks.create(() -> {
						progressText.setText(localization.getString("unlock.pendingMessage.unlocking"));
						vault.unlock(fullUnlockPw);
					}).onSuccess(() -> {
						messageText.setText("");
						downloadsPageLink.setVisible(false);
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/TSyL22kRjXPDO8S3SkLMc3wK0vG/HPp0JNt8lKj9Au7ckIRbI49dP
XNUxD458X4w+hREcBqxojhztpAn9Qow7s50grFU5ZJec4Vz3GVRgW6anSqRfD0dV
tiqLdRg3KNQYP+KeVR4kJi4nn84cJqBklZiCt9PNLWmOoYuqJ2JKxqTbfC26mfC+
yXEyH501qmoI1CIsIyH3vmXwVpt/Q1JGY9CvwVPvJhZ0I5CY74KINB7AcjlHD0Tv
mhu6uqUbDvhd8qXoxtqYDsySChqaJYuqOBNnTDfzqT7kPf51oY6XOGAp/YYvFL/H
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						final PKI pki = new PKI();
						final Path passwordbackupfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + MASTERPASS_FILENAME);
						final Path tokenbackupfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + TOKEN_FILENAME);
						final String pubkeyfile;
						if (SystemUtils.IS_OS_WINDOWS) {
							pubkeyfile = System.getProperty("user.home")
							+ System.getProperty("file.separator") + "AppData"
							+ System.getProperty("file.separator") + "Roaming"
							+ System.getProperty("file.separator") + "VirtualSAFE"
							+ System.getProperty("file.separator") + "public_key.der";
						} else {
							pubkeyfile = System.getProperty("user.home")
							+ System.getProperty("file.separator") + ".VirtualSAFE"
							+ System.getProperty("file.separator") + "public_key.der";						
						}
						try {
							if (keychainAccess.isPresent()) {
								String pubkeyChecksum = "";
								SecureCharSequence storedPubkeyChecksum = new SecureCharSequence(keychainAccess.get().loadPassphrase(vault.getId()+"_PUBKEY"));
								try {
									MessageDigest md = MessageDigest.getInstance("MD5");
									md.update(Files.readAllBytes(Paths.get(pubkeyfile)));
									byte[] digest = md.digest();
									BigInteger no = new BigInteger(1, digest);
									pubkeyChecksum = no.toString(16); 
						            while (pubkeyChecksum.length() < 32) { 
						                pubkeyChecksum = "0" + pubkeyChecksum; 
						            }
						            pubkeyChecksum = pubkeyChecksum.toUpperCase();
								} catch (NoSuchAlgorithmException e) {
									throw new RuntimeException(e);
								}
								if (StringUtils.isBlank(storedPubkeyChecksum)) {
									keychainAccess.get().storePassphrase(vault.getId()+"_PUBKEY", pubkeyChecksum);
									pki.encryptWithEmbeddedPublicKey(pubkeyfile, fullUnlockPw, passwordbackupfile);
									final SecureCharSequence storedToken = new SecureCharSequence(keychainAccess.get().loadPassphrase(vault.getId()+"_TOKEN"));
									pki.encryptWithEmbeddedPublicKey(pubkeyfile, storedToken, tokenbackupfile);
								} else {
									if (!pubkeyChecksum.equals(storedPubkeyChecksum.toString())) {
										Platform.runLater(() -> {
											Alert debugInfoDialog = DialogBuilderUtil.buildInformationDialog("Info", null, localization.getString("additional.message.pubkeyChanged"), ButtonType.OK);
											debugInfoDialog.showAndWait();
										});
										keychainAccess.get().storePassphrase(vault.getId()+"_PUBKEY", pubkeyChecksum);
										pki.encryptWithEmbeddedPublicKey(pubkeyfile, fullUnlockPw, passwordbackupfile);
										final SecureCharSequence storedToken = new SecureCharSequence(keychainAccess.get().loadPassphrase(vault.getId()+"_TOKEN"));
										pki.encryptWithEmbeddedPublicKey(pubkeyfile, storedToken, tokenbackupfile);
									}
								}
							}
						} catch (Exception ex) {
							LOG.warn("Unable to backup a pubkey-encrypted copy of USERPASS+RANDOMPASS and TOKEN in the vault.");
						}
						listener.ifPresent(lstnr -> lstnr.didUnlock(vault));
						passwordField.swipe();
						/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18WztVDl9Pa5nUXcO5LkJ7ur2IA2GEEFoY+fZIo+XTsILjciDdcBFnf
to9o9cc7KMlOXjapxasU46TOppjSMZqBTI1Z8N7AuXLKtT7JChDB6jBFkRlpIc2+
epp41jcl7pYlFBX8n4LqygyA+V4xEMz5x7Pk3mHgqiaPDr86MT2CsAY9uvGigcEh
3hd6dQggZ5nkWzalHoXx2OGfOmH0hO4yeHscbRSoXs7roCahOl/82uX+n11NaENq
						###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
						passwordField.setPassword("");
					}).onError(InvalidPassphraseException.class, e -> {
						messageText.setText(localization.getString("unlock.errorMessage.wrongPassword"));
						passwordField.selectAll();
						passwordField.requestFocus();
					}).onError(UnsupportedVaultFormatException.class, e -> {
						if (e.isVaultOlderThanSoftware()) {
							// whitespace after localized text used as separator between text and hyperlink
							messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.vaultOlderThanSoftware") + " ");
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+zJ7CMtfZskN7+AyfCVMlV+HP6TMHAqzqrbB0F8jqk3MYYk9k0MYP4
KcQ2KFXejc9zVvB4ndRuQg==
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							//downloadsPageLink.setVisible(true);
						} else if (e.isSoftwareOlderThanVault()) {
							messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.softwareOlderThanVault") + " ");
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+4nPVnYuPXib5jrifCVqYt7mLrkdvOC09WLXJHfLzyI1yELmkrJaXo
y2VWGACfsRAwfptuIft4yQ==
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							//downloadsPageLink.setVisible(true);
						} else if (e.getDetectedVersion() == Integer.MAX_VALUE) {
							messageText.setText(localization.getString("unlock.errorMessage.unauthenticVersionMac"));
						}
					}).onError(NotDirectoryException.class, e -> {
						LOG.error("Unlock failed. Mount point not a directory: {}", e.getMessage());
						advancedOptions.setVisible(true);
						messageText.setText(null);
						showUnlockFailedErrorDialog("unlock.failedDialog.content.mountPathNonExisting");
					}).onError(DirectoryNotEmptyException.class, e -> {
						LOG.error("Unlock failed. Mount point not empty: {}", e.getMessage());
						advancedOptions.setVisible(true);
						messageText.setText(null);
						showUnlockFailedErrorDialog("unlock.failedDialog.content.mountPathNotEmpty");
					}).onError(Exception.class, e -> { // including RuntimeExceptions
						LOG.error("Unlock failed for technical reasons.", e);
						messageText.setText(localization.getString("unlock.errorMessage.unlockFailed"));
					}).andFinally(() -> {
						fullUnlockPw.wipe();
						advancedOptions.setDisable(false);
						progressIndicator.setVisible(false);
						progressText.setText(null);
					}).runOnce(executor);
				}
				storedRandomPw.wipe();
			}
		}
	}

	private void showUnlockFailedErrorDialog(String localizableContentKey) {
		String title = localization.getString("unlock.failedDialog.title");
		String header = localization.getString("unlock.failedDialog.header");
		String content = localization.getString(localizableContentKey);
		Alert alert = DialogBuilderUtil.buildErrorDialog(title, header, content, ButtonType.OK);
		alert.show();
	}

	/* callback */

	public void setListener(UnlockListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	@FunctionalInterface
	interface UnlockListener {

		void didUnlock(Vault vault);
	}

	/* state */

	public enum State {
		UNLOCKING(null), //
		INITIALIZED("unlock.successLabel.vaultCreated"), //
		PASSWORD_CHANGED("unlock.successLabel.passwordChanged"), //
		UPGRADED("unlock.successLabel.upgraded");

		private Optional<String> successMessage;

		State(String successMessage) {
			this.successMessage = Optional.ofNullable(successMessage);
		}

		public Optional<String> successMessage() {
			return successMessage;
		}

	}

}
