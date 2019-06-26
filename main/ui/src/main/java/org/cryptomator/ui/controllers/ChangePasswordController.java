/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - password strength meter
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18eIMUeo7AL+jIaJJM0D7CYBhHrTt6PvZlO21R76Ly+jGJI4ltbZqXs
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
import static org.cryptomator.ui.util.Constants.MASTERPASS_FILENAME;
import static org.cryptomator.ui.util.Constants.TOKEN_FILENAME;
import static org.cryptomator.ui.util.Constants.TOKENKEY_FILENAME;
import static org.cryptomator.ui.util.Constants.RESTORE_FILENAME;
import java.security.SecureRandom;
import java.math.BigInteger;
import org.cryptomator.ui.util.SecureCharSequence;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.ui.util.PKI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import org.cryptomator.cryptolib.Cryptors;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.KeyFile;
import org.cryptomator.cryptolib.DecryptingReadableByteChannel;
import org.cryptomator.cryptolib.EncryptingWritableByteChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger; 

public class ChangePasswordController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

	private final Application app;
	private final PasswordStrengthUtil strengthRater;
	private final Localization localization;
	private final IntegerProperty passwordStrength = new SimpleIntegerProperty(-1); // 0-4
	private Optional<ChangePasswordListener> listener = Optional.empty();
	private Vault vault;
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+gnV7ymmNl+85kGJYsoP8+oViAwNugztpKI4iT4w8SrV+3eXzdfnZV
UG7co4ov/lkxXqshzp458A==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private final Optional<KeychainAccess> keychainAccess;

	@Inject
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+tFm9dHFZ1g1rX2WloF2wdJyEwUnRgEgPiuvy+pXoxukkm9EQqzoAD
Sbq//wmzvgXQuIacqK33/9tDk/ecdgvMtTKhA/u/fx2bUE9WWj7sUzukNA58sLgK
KdWTe5ZBroRKHofKb6Kvuomn22DbnDds3VWHFvrCv6wsb71gboEG517As8jOxQ5v
1wkLbo751VWg0U+aDoI1Bhj2kJh4YbdqhbmED0END8c=
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	public ChangePasswordController(Application app, PasswordStrengthUtil strengthRater, Localization localization, Optional<KeychainAccess> keychainAccess) {
		this.app = app;
		this.strengthRater = strengthRater;
		this.localization = localization;
		this.keychainAccess = keychainAccess;
	}

	@FXML
	private SecPasswordField oldPasswordField;

	@FXML
	private SecPasswordField newPasswordField;

	@FXML
	private SecPasswordField retypePasswordField;

	@FXML
	private Button changePasswordButton;

	@FXML
	private Text messageText;

	@FXML
	private Hyperlink downloadsPageLink;

	@FXML
	private Label passwordStrengthLabel;

	@FXML
	private Region passwordStrengthLevel0;

	@FXML
	private Region passwordStrengthLevel1;

	@FXML
	private Region passwordStrengthLevel2;

	@FXML
	private Region passwordStrengthLevel3;

	@FXML
	private Region passwordStrengthLevel4;

	@FXML
	private GridPane root;

	@Override
	public void initialize() {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18KibrrJvDEeyIJZmIT726gaeZvH2wNh5FF8bQOBFV7xq9gknW0GtbW
DDueKG1hbo7VpRookEDDyw==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		downloadsPageLink.setVisible(false);

		oldPasswordField.textProperty().addListener(this::passwordsChanged);
		newPasswordField.textProperty().addListener(this::passwordsChanged);
		retypePasswordField.textProperty().addListener(this::passwordsChanged);

		passwordStrengthLevel0.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(0), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel1.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(1), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel2.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(2), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel3.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(3), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel4.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(4), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	private void passwordsChanged(@SuppressWarnings("unused") Observable observable) {
		boolean oldPasswordEmpty = oldPasswordField.getCharacters().length() == 0;
		boolean newPasswordEmpty = newPasswordField.getCharacters().length() == 0;
		boolean passwordsEqual = newPasswordField.getCharacters().equals(retypePasswordField.getCharacters());
		changePasswordButton.setDisable(oldPasswordEmpty || newPasswordEmpty || !passwordsEqual);
		passwordStrength.set(strengthRater.computeRate(newPasswordField.getCharacters().toString()));
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	@Override
	public void focus() {
		oldPasswordField.requestFocus();
	}

	void setVault(Vault vault) {
		this.vault = Objects.requireNonNull(vault);
		// trigger "default" change to refresh key bindings:
		changePasswordButton.setDefaultButton(false);
		changePasswordButton.setDefaultButton(true);
		messageText.setText(null);
	}

	// ****************************************
	// Downloads link
	// ****************************************

	@FXML
	public void didClickDownloadsLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/downloads/");
	}

	// ****************************************
	// Change password button
	// ****************************************

	@FXML
	private void didClickChangePasswordButton(ActionEvent event) {
		downloadsPageLink.setVisible(false);

		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19T+cwXvHOFk00ptj0b3bLCbiE7IIrxlVeyJX4GZiFioyMVu3cYAxrA
q1IQ8eC6RBaqg8rvT+yWNA==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		if (!keychainAccess.isPresent()) {
			LOG.error("Error accessing system keychain.");
			messageText.setText(localization.getString("additional.errorMessage.cannotAccessSystemKeychain"));
			return;
		}

		try {
			final SecureCharSequence storedRandomPw = new SecureCharSequence(keychainAccess.get().loadPassphrase(vault.getId()+"_RANDOMPASS"));
			if (StringUtils.isBlank(storedRandomPw)) {
				LOG.error("Unable to change vault password. Cannot retrieve RANDOMPASS from system keychain.");
				messageText.setText(localization.getString("additional.errorMessage.cannotChangeVaultPassword") + "\n" + localization.getString("additional.errorMessage.cannotFetchDataFromKeychain"));
				return;
			} else {
				vault.changePassphrase(oldPasswordField.getCharacters()+storedRandomPw.toString(), newPasswordField.getCharacters()+storedRandomPw.toString());
				
				LOG.info("Vault password changed.");
				messageText.setText(localization.getString("additional.vaultPasswordChanged"));

				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18Wz+zm35jWM5YJT+9ke3HGsfP3D93Ot3asSDdUOc+dUgP3cVuoqYPb
BPLnsPdPYTdiNFzsuJrkW5hr2z9ee4tZjH7lGEz3mgo=
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				final SecureCharSequence passphrase = new SecureCharSequence(newPasswordField.getCharacters()+storedRandomPw.toString());
				final PKI pki = new PKI();
				final Path passwordbackupfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + MASTERPASS_FILENAME);
				final Path tokenbackupfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + TOKEN_FILENAME);
				final Path tokenkeyfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + TOKENKEY_FILENAME);
				final Path restorefile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + RESTORE_FILENAME);
				final SecureRandom random = new SecureRandom();
				final BigInteger randomtoken = new BigInteger(80, random);
				final SecureCharSequence tmptoken = new SecureCharSequence(keychainAccess.get().loadPassphrase(vault.getId()+"_TOKEN"));
				final SecureCharSequence restoretoken;
				if (StringUtils.isBlank(tmptoken)) {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/SJmTn83ZeXrB2C+rVAsx0soNQnkFmFCkZdfvJ8RICaBUziRMeslGX
Co1YppCzfB4Ks/7i9HgxmllfWD7wJQ1l7IxSOdFfA9Ng1boFWfoc8x/5wz3IlGXc
TqNgrlu8o/0nJGoLYFJzsQ==
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					restoretoken = new SecureCharSequence(randomtoken.toString(32).toUpperCase());
				} else {
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19AGu9u9Blfzn3Wipbm20OfRtnq8EYYO5h9kIo99hvyJs94OQHepu5l
7+Zv8KPgarcVC/INrEvSnA+cYCP85nEV1OTDC6XBzt/iNhJa1SEcOevjyfUwT+VM
W8SZbfgj/H6eAOrqkK6ARcuEFAcHo5evibh0ICq0MgY=
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					restoretoken = tmptoken;
				}

				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/pSbSs/47xF+xvmmFPv82e3be9gGvHQA3ey6LWxhhpUPCcK1rv+o96
S60hkTJo87njdoQfujZAYPSp9YZcwObMoPUcAyEwiJIMZfmFkjuGzL4ejDKuJG/O
xspTQe27uexwFWAD/xvyaQ==
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
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
					pki.encryptWithEmbeddedPublicKey(pubkeyfile, passphrase, passwordbackupfile);
					/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+jgDvKbmQqa6sMKtTcRx+oAIWUctPCR0Z0XzGmq8qe2RJmwBlgDYX3
fLnKvr9pBetYq+d0qT2Ut59v6kb8vlJeEVVKIgQffdoJc7tfI18Wmri5USukERR6
XkUg1Vxloh2KdHFnVtCAmFDXb8KabzI3305ECKHeaDw5zJADBd8Wrl8kuaXt87YD
B+d691HXpjWi8JMVhRBkpg==
					###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
					if (StringUtils.isBlank(tmptoken)) {
						pki.encryptWithEmbeddedPublicKey(pubkeyfile, restoretoken, tokenbackupfile);
					}
				} catch (Exception ex) {
					LOG.warn("Unable to backup a pubkey-encrypted copy of USERPASS+RANDOMPASS and TOKEN in the vault.");
				}

				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/YoIOYEI8gxFd9+SIcAmhjX+P0xodim8u7UhH3N0kYtq23u38H9L8T
yLOgo3vDuPmTwWbzsU/dU/TQX1OXuWa5PIlnMWQM+J+XoxugeopFEDxkSukRL3KH
V1+KpcwLvhZVCGcLzjQJaj1xKM7rxr1oNf6UUA9gdmOzkZbgPwHB2aadBA8DJC/y
057lxyg5ztRN6RWjcEjeQJRB3YuBy3v7iMkhjbpmQv1bgYS7Hw3B5FHAr1YB6p8Z
MM6mJHFPAKRDanki4q+4uw1D2Jy65WfzeaJA+QT9vSmhIEcOmzYb6WklsSrGZObk
Ky5cjGl+MYXZyy9QbEPOaOvtg78Hdyz1S6Px0YVsjd1y1NxvYFz8zh17uQtSE0XI
ya36yNMZpYBa/2AIEpnjUBjcafdAwDv5VU9asQeMMlxLGzkDAwVHQmpb5Yk7KpbO
nqAZ/tr2UzQuAnxfv4g5Ng==
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				try {

					Cryptor cryptor = Cryptors.version1(random).createNew();
					KeyFile tokenkeyFile = cryptor.writeKeysToMasterkeyFile(restoretoken, 42);
					byte[] tokenkeyFileContents = tokenkeyFile.serialize();
					Files.write(tokenkeyfile, tokenkeyFileContents,  StandardOpenOption.WRITE,  StandardOpenOption.CREATE,  StandardOpenOption.TRUNCATE_EXISTING);

					ByteBuffer plaintext = Charset.defaultCharset().encode(storedRandomPw.toString());
					SeekableByteChannel ciphertextOut = Files.newByteChannel(restorefile,  StandardOpenOption.WRITE,  StandardOpenOption.CREATE,  StandardOpenOption.TRUNCATE_EXISTING);
					try (WritableByteChannel ch = new EncryptingWritableByteChannel(ciphertextOut, cryptor)) {
						ch.write(plaintext);
					}
				} catch (Exception ex) {
					LOG.warn("Unable to backup a token-encrypted copy of RANDOMPASS in the vault.");
				}

				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX198B6Y/1mvTJypSlIm8AzUTB/RLToUZmIHv9WJGnDEsVL187R3Vr6B4
0m24aIU67dlmfHYcIqhvAth6bRF7thldnY9n+MSYmaeV2olgwHbvdFXL0rpMWsne
v5vHzoqmYaRsorxFVFKdUMb1D+Xn5itIOGiE39+Vj2VY85kNV258SDLa7yHIlmX1
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				LOG.info("Storing TOKEN in keychain.");
				if (keychainAccess.isPresent() && StringUtils.isBlank(tmptoken)) {
					keychainAccess.get().storePassphrase(vault.getId()+"_TOKEN", restoretoken);
					try {
						MessageDigest md = MessageDigest.getInstance("MD5");
						md.update(Files.readAllBytes(Paths.get(pubkeyfile)));
						byte[] digest = md.digest();
						BigInteger no = new BigInteger(1, digest);
						String pubkeyChecksum = no.toString(16); 
			            while (pubkeyChecksum.length() < 32) { 
			                pubkeyChecksum = "0" + pubkeyChecksum; 
			            }
			            pubkeyChecksum = pubkeyChecksum.toUpperCase();
						keychainAccess.get().storePassphrase(vault.getId()+"_PUBKEY", pubkeyChecksum);
					} catch (NoSuchAlgorithmException e) {
						throw new RuntimeException(e);
					}
				}
				storedRandomPw.wipe();
				tmptoken.wipe();
				listener.ifPresent(this::invokeListenerLater);
			}
		} catch (InvalidPassphraseException e) {
			messageText.setText(localization.getString("changePassword.errorMessage.wrongPassword"));
			Platform.runLater(oldPasswordField::requestFocus);
		} catch (UncheckedIOException | IOException ex) {
			messageText.setText(localization.getString("changePassword.errorMessage.decryptionFailed"));
			LOG.error("Decryption failed for technical reasons.", ex);
		} catch (UnsupportedVaultFormatException e) {
			//downloadsPageLink.setVisible(true);
			LOG.warn("Unable to unlock vault: " + e.getMessage());
			if (e.isVaultOlderThanSoftware()) {
				messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.vaultOlderThanSoftware") + " ");
			} else if (e.isSoftwareOlderThanVault()) {
				messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.softwareOlderThanVault") + " ");
			}
		} finally {
			oldPasswordField.swipe();
			newPasswordField.swipe();
			retypePasswordField.swipe();
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18ZKNR9QW3k2leFnAi+BjvlkLHAuBYr2DWAnp1uKef17TwJOaasO6lu
03PEShoo0FSvkYgrqCrejP5xnSSxx5vBtM6kze61yymLVW2DtQ8qAsMWbnMmp/Xw
cktlfIoz72XNE+cyjPPogdmpOS28cUSlu8ViXywmlBoaiWF+5a6MyLAtchZHlCDR
s8eiy91y53V9GL434fQd4KKm0dTVQPp9GDZfa7/Kl5k=
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			oldPasswordField.setPassword("");
			newPasswordField.setPassword("");
			retypePasswordField.setPassword("");
		}
	}

	/* Getter/Setter */

	public ChangePasswordListener getListener() {
		return listener.orElse(null);
	}

	public void setListener(ChangePasswordListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	/* callback */

	private void invokeListenerLater(ChangePasswordListener listener) {
		Platform.runLater(() -> {
			listener.didChangePassword();
		});
	}

	@FunctionalInterface
	interface ChangePasswordListener {
		void didChangePassword();
	}

}
