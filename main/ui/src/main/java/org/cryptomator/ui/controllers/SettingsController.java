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
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Volume;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19zv9747ZB+Q0ue6scAGtTLwkBAp11Y57KIn+jSGDYZ76s2/V+8/3+m
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.cryptomator.ui.util.Constants.VSAFEVERSION;

@FxApplicationScoped
public class SettingsController implements ViewController {

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19LYe4apAD1YRuh5kep2IheRVmA0vQ/7IUft0Yzi+eqxuKXI9wahJTC
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);
	private static final CharMatcher DIGITS_MATCHER = CharMatcher.inRange('0', '9');

	private final Localization localization;
	private final Settings settings;
	private final Optional<String> applicationVersion;

	@Inject
	public SettingsController(Localization localization, Settings settings, @Named("applicationVersion") Optional<String> applicationVersion) {
		this.localization = localization;
		this.settings = settings;
		this.applicationVersion = applicationVersion;
		this.webdavSettings = new Group();
	}

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/a+PHL/9XLFBi2LC7tRiGSf+Z12U8Mowgu23Xfyc8tdk1rTJRvXpi5
MQXkZvHpWcuJsWtprrk6FTo7BZM/DhaeRJtIiuGoAFXbqx32OJqIr+vv18EXSOyi
eyEy2GPj1tpn+rQ1KOVhvQ==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */

	private Group webdavSettings;

	@FXML
	private Label portFieldLabel;

	@FXML
	private TextField portField;

	@FXML
	private Button changePortButton;

	@FXML
	private Label versionLabel;

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18z+kRGJP1/NkJmt4c1aEVOt0XCojfsrRRjxMYcoflRitGF//7juTg+
LuMRFaBfQn0b/zDa+dNxdw==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	@FXML
	private Label wwwLabel;
	
	@FXML
	private Label copyrightLabel;

	@FXML
	private Label prefGvfsSchemeLabel;

	@FXML
	private ChoiceBox<String> prefGvfsScheme;

	@FXML
	private ChoiceBox<VolumeImpl> volume;

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19XRXsX7hJ2X+4n6MaKR3X9sD+3Enc+c6EZPZkNp7UPdAf07grxp/dL
wCHOO9jGT+NJONuHmwG8ZuCsqscuMEbPKh6TA1LVXBX8DH9Mtb+Ba4/XMP2s6Q90
OOtp/yn7yYUiKix94E8dTA==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+fma5/jrrMvi204uIgG3D7XNL5yQYKPQOcs/TL75V6xpc9Mat9V5Ua
tDH2hUQkBHcHxbhn+z5hnVvBsyKO58+HykM/TVcaYTOtjPdi5Q4DWcKDal6I4MGR
Nas1Wl/nnSO7h04Hi54GomEFRdD/XJrgjWF6gt2+/JjGzrBqcM8KWHoaIG2/vArt
Yuhn3LS0w/HCBJWrLkUZRw==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */

	@FXML
	private VBox root;

	@Override
	public void initialize() {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18xZAI/XZ9IxK0lbgV+ZfscsL4Gk1XG/R57bXJtfYMNxzINjvN5bOJ0
P1J8htlFdknbXGeL9yUMN6UaA3TYS8sL/0i9jhoqvYcND9FmMymm9T+RwNR4kvn0
cvDPiubXqNNcpNdKT6OVD5PkqiHzRHC96FjwI/l/auL6pgrzoL27pBGM1EVbHexC
PVnZ0AyeBOhXK44mEqmpZz9E7b7zwmCYKn/8j5kNag4VMk9dbPi0PoaoCjqT3ipw
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		versionLabel.setText(String.format("VirtualSAFE "+localization.getString("settings.version.label"), VSAFEVERSION));
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18cg9HGNK1XrGAS3DbvbgzyVfl/5Rp2OwFsgI7EjtUEyGP50dq3POTp
Fqk3/YBBVrVQ7Z1BzBBDag==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		wwwLabel.setText("www.virtualsafe.cloud");
		copyrightLabel.setText("Copyright e-Lock Corporation");
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+A8gmBDJNq3pZHkqEhGa52sN+fQhHfo7Cz/Mk5yG9pkTJBMRCfeT3x
J8YS60jIvPZfRmqt5/fqBk6LlW6PQlaGfaFpX2Wt2FBxd/ev+WMNC+fgZL1/5U2p
2PniLaGNtVqC59kzrh2u+5Js9/CB3f5ieIESgYGmNJdnqalJ5MfHsmZAx11dg+p1
/sYsRJgTCBmhjgaaUb82jaDF1dZfJusfK6q/87UnSybhIGnlcLfQ+EAS3t9w+dPE
B83h1NahhxehmBcN8pbwYmANpKZXQ6la84CnSZoHTQskgzBYMPwS51INFG/lTnnc
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */

		//NIOADAPTER
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18bU1z0PxsADvjt+HnbiT8bJCNe8ob6e87cb67esP3qAEQ4Hp5xhFLr
TOXy0KXgZfXLqcqBQn9i14ccrtHyOSuzFmqXVkn2sbmnqFyqSr6i8qRHkDeCQR5f
/1Yh6jRjul7VDJwKgSsF13TfM70ME+QGBG+/evBcolDNL/ZDOn7X57d9Wkbz4xqG
7+PHK7LVTbx+5eNnBbj4BQdMI4W4paZjLkRHnbh74J+zFaePkzsrfBfWzVtnVLL9
0a9BfIhSD5zXnFpRt0qk08rp5D908NtZE3RoP2WLKkO7L4OH1cQq7RqD9kAJ+oFP
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		volume.getItems().add(VolumeImpl.WEBDAV);
		volume.setValue(VolumeImpl.WEBDAV);
		volume.setConverter(new NioAdapterImplStringConverter());
		volume.valueProperty().addListener(this::setVisibilityGvfsElements);

		//WEBDAV
		webdavSettings.visibleProperty().bind(volume.valueProperty().isEqualTo(VolumeImpl.WEBDAV));
		webdavSettings.managedProperty().bind(webdavSettings.visibleProperty());
		prefGvfsScheme.managedProperty().bind(webdavSettings.visibleProperty());
		prefGvfsSchemeLabel.managedProperty().bind(webdavSettings.visibleProperty());
		portFieldLabel.managedProperty().bind(webdavSettings.visibleProperty());
		portFieldLabel.visibleProperty().bind(webdavSettings.visibleProperty());
		changePortButton.managedProperty().bind(webdavSettings.visibleProperty());
		portField.managedProperty().bind(webdavSettings.visibleProperty());
		portField.visibleProperty().bind(webdavSettings.visibleProperty());
		portField.setText(String.valueOf(settings.port().intValue()));
		portField.addEventFilter(KeyEvent.KEY_TYPED, this::filterNumericKeyEvents);
		changePortButton.visibleProperty().bind(settings.port().asString().isNotEqualTo(portField.textProperty()));
		changePortButton.disableProperty().bind(Bindings.createBooleanBinding(this::isPortValid, portField.textProperty()).not());
		prefGvfsScheme.getItems().add("dav");
		prefGvfsScheme.getItems().add("webdav");
		prefGvfsScheme.setValue(settings.preferredGvfsScheme().get());
		prefGvfsSchemeLabel.setVisible(SystemUtils.IS_OS_LINUX);
		prefGvfsScheme.setVisible(SystemUtils.IS_OS_LINUX);

		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18GL0iibXzimB7JDGJceZYfkIEmub78QvkxX0b+TW1OvFKFULaDvqGD
U15Qk13XpMazS3HAzAqbKLJp/GoOfuPgbAu3mnGmaYxkjtwfvpJcLBiur2zeQaNY
YyZPtdh15hZI+bFA12A4tA1yCGY9W9lwxxctgdCgzsKKHthYKD8774F4wt85Bmvc
xj/j6kM3czo8SYTqaYx8fdiYUhxzh2SvY/7gd2ZOQRu+kvIig8jYq23siB0Z4Gap
2UVTtxi7ttXQrZ7uo48ZNbbGGaytnpmdrpKNtBYWbvDUD59sSigYgOsAVNI7UgtR
lFDQ+5GdgcPg5C0yVjYsdQ==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */

		//settings.checkForUpdates().bind(checkForUpdatesCheckbox.selectedProperty());
		settings.preferredGvfsScheme().bind(prefGvfsScheme.valueProperty());
		settings.preferredVolumeImpl().bind(volume.valueProperty());
		//settings.debugMode().bind(debugModeCheckbox.selectedProperty());
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	@FXML
	private void changePort() {
		assert isPortValid() : "Button must be disabled, if port is invalid.";
		try {
			int port = Integer.parseInt(portField.getText());
			settings.port().set(port);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Button must be disabled, if port is invalid.", e);
		}
	}

	private boolean isPortValid() {
		try {
			int port = Integer.parseInt(portField.getText());
			return port == 0 // choose port automatically
					|| port >= Settings.MIN_PORT && port <= Settings.MAX_PORT; // port within range
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void filterNumericKeyEvents(KeyEvent t) {
		if (!Strings.isNullOrEmpty(t.getCharacter()) && !DIGITS_MATCHER.matchesAllOf(t.getCharacter())) {
			t.consume();
		}
	}

	private void setVisibilityGvfsElements(@SuppressWarnings("unused") Observable obs, @SuppressWarnings("unused")Object oldValue, Object newValue) {
		prefGvfsSchemeLabel.setVisible(SystemUtils.IS_OS_LINUX && ((VolumeImpl) newValue).getDisplayName().equals("WebDAV"));
		prefGvfsScheme.setVisible(SystemUtils.IS_OS_LINUX && ((VolumeImpl) newValue).getDisplayName().equals("WebDAV"));
	}

	private boolean areUpdatesManagedExternally() {
		return Boolean.parseBoolean(System.getProperty("cryptomator.updatesManagedExternally", "false"));
	}

	private static class NioAdapterImplStringConverter extends StringConverter<VolumeImpl> {

		@Override
		public String toString(VolumeImpl object) {
			return object.getDisplayName();
		}

		@Override
		public VolumeImpl fromString(String string) {
			return VolumeImpl.forDisplayName(string);
		}
	}

}
