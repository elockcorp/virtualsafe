/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - password strength meter
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Objects;
import java.util.Optional;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+yA07PaYzbPbobLwqRAfTeOp9y6TnyAt4v0MeSo0sJQCwKg5FiNzdf
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
import org.apache.commons.lang3.SystemUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger; 

public class InitializeController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);

	private final Localization localization;
	private final PasswordStrengthUtil strengthRater;
	private IntegerProperty passwordStrength = new SimpleIntegerProperty(-1); // strengths: 0-4
	private Optional<InitializationListener> listener = Optional.empty();
	private Vault vault;
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX182YxV7ckwqsSA4PhmRONdY3M57T6RruU4hHGvLfqG+J4KC3UlGH4sI
1EpxKcBU9XPZHuLQdcc4DA==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private final Optional<KeychainAccess> keychainAccess;

	@Inject
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18PLXf78AJfXPoodWCqjDX2ufOPDLhKmADRiQov1tqSWktOJ1pTeum/
AmYbS77ikBPsvSjIUCyqCHr8gbSrAOOhYvpRA2Rnrnz1pqxgo4/oK7VEFPB1FAVY
drGBMbe/H9PZLbpNAWQKio1/l96VvLV9tLztkWwgbO4ey9X8f/n2yMrkKRXNEDFA
z4u0WAfD+WqXhfqOdEP0KA==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	public InitializeController(Localization localization, PasswordStrengthUtil strengthRater, Optional<KeychainAccess> keychainAccess) {
		this.localization = localization;
		this.strengthRater = strengthRater;
		this.keychainAccess = keychainAccess;
	}

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private SecPasswordField retypePasswordField;

	@FXML
	private Button okButton;

	@FXML
	private Label messageLabel;

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
		passwordField.textProperty().addListener(this::passwordsChanged);
		retypePasswordField.textProperty().addListener(this::passwordsChanged);

		passwordStrengthLevel0.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(0), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel1.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(1), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel2.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(2), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel3.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(3), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel4.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(4), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	private void passwordsChanged(@SuppressWarnings("unused") Observable observable) {
		boolean passwordsEmpty = passwordField.getCharacters().length() == 0;
		boolean passwordsEqual = passwordField.getCharacters().equals(retypePasswordField.getCharacters());
		okButton.setDisable(passwordsEmpty || !passwordsEqual);
		passwordStrength.set(strengthRater.computeRate(passwordField.getCharacters().toString()));
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	@Override
	public void focus() {
		passwordField.requestFocus();
	}

	void setVault(Vault vault) {
		this.vault = Objects.requireNonNull(vault);
		// trigger "default" change to refresh key bindings:
		okButton.setDefaultButton(false);
		okButton.setDefaultButton(true);
	}

	// ****************************************
	// OK button
	// ****************************************

	@FXML
	protected void initializeVault(ActionEvent event) {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/KVCNul///8zOdXvYHg12RZIgF97z119ijMh3fsNrVfTKFsxAJcCpW
sC3txQupD6lNaj0YrsZ+Xy6zN2/y3WCmpm64HTrkseojBg4tMpgD78FbNSCiOv6M
jqlBUT2JQKvohrPKIXaB6pkp3NvsGWACLJDy2etm5jiUthcS3F3gWnXuIHVNjCwT
9uIGgCsbLxsVu0XCT+0/GTQ7YvhmU3AIzZBKD0MhUEdjeRnzfZKnSAtdAKGWoArM
DCwfCPsnrGto/3tTfXf/qxcaq7zv1hakh9znE0GuIzPjzx/aUp/Lh/+Vb581ggkA
18arnyOMH4klX/PUsSW++jMIAi7u0r+f9H+jUFWB11aH/6n8O1wSJBronzloIQvz
hRra4ZFNfu+SVnLinF3b7nAU8SqCp//rE5HDh3/OqXjPL9ijQkjdO/rOZLl5Nba/
RLNHuVtshwbkYNRPmcVQSa3ZdNPFWboeXZqrpcKxDPRGpccoq9F11F/srgZKz4XL
KvJ4y6MP96egACFYpjXx8ULZFvFF3/cQoJaIuZRYiCGcjz6Cycl0FlmUuXPY7rXL
neqmMqkXH6ptiwwtVT9JXgI/kwFYPDRji716USJDgDs9t6EH0LNgoh7VmRfgJKRI
zceDDiyT6Iuszbmu4uuBdBYpbKgyhDKM4rGk/0I6f8vrCBm70ZxIdu2lcABPadOD
nIDwoaxOvoUKCC/SywM3Zywfb0zAJxVTcfkH2qC2mj/RlB9USGPHRGPQqNKVmQjY
/wQz6ZynX29yP/ekh77Kxg==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		final SecureRandom random = new SecureRandom();
		final BigInteger randompass = new BigInteger(250, random);
		final BigInteger randomtoken = new BigInteger(80, random);
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+AFz/TMTBttF2kcKOkH2XwAWvmCriUCKfWk9zLAFa9P/nL/rMcPUJC
bNebM0K7Kp/22KAHo6evKLpogzOzZ1IKbO8s0zkiorU=
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		final SecureCharSequence passphrase = new SecureCharSequence(passwordField.getCharacters()+randompass.toString(32));
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19SKJzfXRgKGtfOPBjl3kbDlXQPd79+wWKKZfZJiKhOSg6RvYhnmZWM
FalTkpbbsGgUwRbwT5yh8EEY4RiRKVS148GGGOZ/NxEQjPDnC9ViTpiklB46KBAr
vX+bI1VyHrkTLoOIZiztgA==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		final SecureCharSequence restoretoken = new SecureCharSequence(randomtoken.toString(32).toUpperCase());
		final PKI pki = new PKI();
		final Path passwordbackupfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + MASTERPASS_FILENAME);
		final Path tokenbackupfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + TOKEN_FILENAME);
		final Path tokenkeyfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + TOKENKEY_FILENAME);
		final Path restorefile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + "LockedVirtualSAFE" + System.getProperty("file.separator") + RESTORE_FILENAME);

		try {
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/V/a6b44/lXfHVTTV+8XJwDTMA6de4kAdYQntojEOiGYvmcnJ4SGNR
cZ6cNcTm4QQC/IGPlwtPAnb+VRvzH2xPvxlTsRCITxc=
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			//LOG.debug("passphrase: {}", passphrase.toString());
			vault.create(passphrase);
			LOG.info("Vault initialized with MASTER PASSWORD.");

			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/Qiruz1qcDAuwx37Gt/OlWTANhpL7G08pYSBbFPM4tZVtf34NoBY/d
U4py510NHaMNCAZv7WFeyptJbfIjHGglH+O/jZIemYMeYaPd10zWRCTkxcvlSn9P
3sbtFdGjpife6Be4WZ/J1Q==
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
U2FsdGVkX18Ai9xmFhtZ3ZxkwLe/AVayhGU+9jmmPtOJIipVcQ0IlErLsdQX+xNg
67ueDDWSCw8IC/VXJUrAHD5LiCmTBrpA8fAql9fDGGn9tZELORMx3+monCR7Vr3N
tFhe6p8XbbIy8fqPCY+GajyqzW1qeEnpk6AKJrQEgohn/fHigaVCyc1g++oBzZhA
S1mh3M3a17kK3DCnWztBIA==
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				pki.encryptWithEmbeddedPublicKey(pubkeyfile, restoretoken, tokenbackupfile);
			} catch (Exception ex) {
				LOG.warn("Unable to backup a pubkey-encrypted copy of USERPASS+RANDOMPASS and TOKEN in the vault.");
			}

			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+9Xehm4m9RPdusPEVdq1+8Zu+oONsGO7x9luG7vtfF8Z1cjp/gBfmX
toVDRqq/nI4EeVYpRD5YeM9NCNyNQLIG/fawfXrhui1plrb/JysC9ZuP6Go+c0i3
gDF0xuORFPX9hrtQ4hvE6/17EfDEpBLeBmVF7Vcmu/IDzggMD4QJuOb1zIsuIlZg
Wk9HSALWuIQ36Jlr/jE/OpaT/NdWwg+3Lfzw5fMYdELG9Ucg5ZS6jHTAGZLQY1gL
ubwioFnOVeJL8ox76FdbCEjot4A0hcPi994MsKiXPNg9XvXryW1CzMcO6UrMNZVT
PVzdw56ZPuctZhHQvML/jR4tqF0EzDAbUlLy90ybngC42fGK8tFD6GcLVxpC+WtT
t5SBtVxUtvU6LoCT2+6mOPHZVhWLDNPa1Qj49LffJjqqnMSllGiamrowZlXACttn
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			try {
				Cryptor cryptor = Cryptors.version1(random).createNew();
				KeyFile tokenkeyFile = cryptor.writeKeysToMasterkeyFile(restoretoken, 42);
				byte[] tokenkeyFileContents = tokenkeyFile.serialize();
				Files.write(tokenkeyfile, tokenkeyFileContents,  StandardOpenOption.WRITE,  StandardOpenOption.CREATE,  StandardOpenOption.TRUNCATE_EXISTING);

				ByteBuffer plaintext = Charset.defaultCharset().encode(randompass.toString(32));
				SeekableByteChannel ciphertextOut = Files.newByteChannel(restorefile,  StandardOpenOption.WRITE,  StandardOpenOption.CREATE,  StandardOpenOption.TRUNCATE_EXISTING);
				try (WritableByteChannel ch = new EncryptingWritableByteChannel(ciphertextOut, cryptor)) {
					ch.write(plaintext);
				}
			} catch (Exception ex) {
				LOG.warn("Unable to backup a token-encrypted copy of RANDOMPASS in the vault.");
			}

			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18Y9IoI1qbgYVuCyGLza1IhvalVpSJR82e5TONDm1QqY0M97Ois8Wbb
w8MboPwoLYh4EIQWgB0/TinyGcpU3k2I1KFwefoT8+driEY9lyM+Ek+mS+vOsVqP
dQrh8+k/y8fZPFV441q58uY6l+NXNyUHaqoWJWfLBzNkwYlHwla40iomVUPyR3Cf
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			LOG.info("Storing RANDOMPASS & TOKEN in keychain.");
			if (keychainAccess.isPresent()) {
				keychainAccess.get().storePassphrase(vault.getId()+"_RANDOMPASS", randompass.toString(32));
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
			listener.ifPresent(this::invokeListenerLater);
		} catch (FileAlreadyExistsException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.alreadyInitialized"));
		} catch (IOException ex) {
			LOG.error("I/O Exception", ex);
			messageLabel.setText(localization.getString("initialize.messageLabel.initializationFailed"));
		} finally {
			passwordField.swipe();
			retypePasswordField.swipe();
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+Gau8om5snz558NHexCj9buVc8PxJ1nY81/nHZyCKCjBXbyw+zzWJ5
s8pk3oel7qrbwMqteAP3DXYjrxsXLrw4ZCgt33IcIlkM1e+0r7fm1JsWyQlxXsVK
bhIE2aWZPeqsrttNNZ7gz/uTwBhtzpUdPzXpoWvnWc8LQ5wI5E6Jm3ntZdZVmkHc
+6FLoA1Vd9ysK5lWX/ylOjGkXQQxB0NoNhE7jiSUAPw=
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			passwordField.setPassword("");
			retypePasswordField.setPassword("");
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/TlB891opYdNCSow/d00Vf3ktNR6DgHnGVkxZdigwdQbiJck34N7IO
kTPSCJHHQoyrl5yCj4EAFA==
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			passphrase.wipe();
		}
	}

	/* Getter/Setter */

	public InitializationListener getListener() {
		return listener.orElse(null);
	}

	public void setListener(InitializationListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	/* callback */

	private void invokeListenerLater(InitializationListener listener) {
		Platform.runLater(() -> {
			listener.didInitialize();
		});
	}

	@FunctionalInterface
	interface InitializationListener {
		void didInitialize();
	}

}
