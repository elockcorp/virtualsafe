/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.util.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.cryptomator.ui.util.DialogBuilderUtil.buildYesNoDialog;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/mAn/pkrzeq0MSB/re0XgjwBKTniLweQGzhvk/MKw0kiBg3w8qOcKs
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
import org.cryptomator.ui.util.VaultState;

@FxApplicationScoped
public class WelcomeController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(WelcomeController.class);

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/4oVPKj0Z4NOYweixlmhPtq7QDGFkl5hAhPkC2zZShmK4OdyiSyz5Y
sOUu5bGINwIa6MKSKI8Puw==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	public static VaultState.State state = VaultState.State.UNDEFINED;
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18i7SAYSLecuFiF00CmmW1yT1cvcEnj8oueqZ1RFzlcv148PPZgiY65
hB3gPGy+9O4ruqnA7mspVQ==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private Optional<WelcomeListener> listener = Optional.empty();

	private final Application app;
	private final Optional<String> applicationVersion;
	private final Localization localization;
	private final Settings settings;
	private final Comparator<String> semVerComparator;
	private final ScheduledExecutorService executor;
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/9JxhxS9rtjTqDc3UnuE7AfFNwDc3ikNucmb0Zi+gT1kGZ7le6qWGg
pBxdl1MXpG2ZmZFMtVgmSA==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private final VaultState vaultState;

	@Inject
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18Nbzz4wgx4BII3VaL0zsI/DmKb9lZ20/ILg8WjlwxUMU8L6zJBmIj5
k69JYiJcQg2mvJ5/fRcF9OBFtD/tuxMTSW2paL779YsIlJss1ZcAAARbBRqmkrsK
hs/SNTkbvuwivJ5iD5dAGARptjtKU7WPYx9GwE4BOVqehM0a2Tm0zn1tTU85w3sD
eUUW4pZJlrtaGKPTl+DblKK5MDsaDLhyEUdEJs8YcBKPFxYZ6gcsMqeSOnGmW/wn
f1X4o9bLtXcxphbNLTXSO9bOq+zE/pbbfMkBCQlXPkN19SquYLAs4zezgx7ILQ/W
0pfjTC6t9hAGGO1cFXg7Q6EFmwGFAi2eHPs4XvaJtRNRAIbFgghxHOm6m9g2r6M/
E6DkiPOG9aHnlJHQK2gLTw==
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	public WelcomeController(Application app, @Named("applicationVersion") Optional<String> applicationVersion, Localization localization, Settings settings, @Named("SemVer") Comparator<String> semVerComparator,
							 ScheduledExecutorService executor, VaultState vaultState) {
		this.app = app;
		this.applicationVersion = applicationVersion;
		this.localization = localization;
		this.settings = settings;
		this.semVerComparator = semVerComparator;
		this.executor = executor;
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19hMl1X/ZN1g6uFPkPLh+hn6IFyo4ZoHpMC6G2BmhBmrihzMAM2ebNE
eza7IyxKiEeMae/fwkPqkA==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		this.vaultState = vaultState;
	}

	@FXML
	private Node checkForUpdatesContainer;

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/RTUZWNEOYkKr/W7/3L3plJ8MAHibU2QwpykwYgvDsIjh1if262q7u
DoQXDEYylxm7LrL/JphiMIQW+UMx2QYkJ6994xUewC4=
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	@FXML
	private Node checkForVaultStateContainer;

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+G2fR20GLyLhHDiG+DUA9/X+64Snei3/H/41yHjxV8LRJZVoAdMwk5
GPMjMd6IMZQTUsB6a+gbT51cRhazoLTvoe4DOaySk24=
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	@FXML
	private Label checkForVaultStateStatus;

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19lHgktnTKhNKhIX+s4pMOhHRCLCK9BXMvMKQs7sm7B9alZcRaAXbgZ
t/YVeyqdCKZwkAZfGO0lgTU8hnBzpzCNxfycaTDqO98=
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	@FXML
	private ProgressIndicator checkForVaultStateIndicator;

	@FXML
	private Label checkForUpdatesStatus;

	@FXML
	private ProgressIndicator checkForUpdatesIndicator;

	@FXML
	private Hyperlink updateLink;

	@FXML
	private VBox root;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19HYB2pD4dVt1+JNYgZ5VbaUAb4s3OiUK36KErSj9CE0uKo69DOA5e2
GkpNAZutXkT9hY9gjYE9bFpvVNqBKOh2xZtuWaWBjcQ=
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		/*
		if (areUpdatesManagedExternally()) {
			checkForUpdatesContainer.setVisible(false);
		} else if (!settings.askedForUpdateCheck().get()) {
			this.askForUpdateCheck();
		} else if (settings.checkForUpdates().get()) {
			this.checkForUpdates();
		}
		*/
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/Y++/WvZJNM6zm9h9n//tiJkzfNy/kEbLBxh7UEQgzhGxkPgIObszW
4l8AKx623qbuVa5XO0MBWQ==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		checkForUpdatesContainer.setVisible(false);

		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19ClqT1oARrNzrKpO1KoVEFQOedTMHTTMb7zsER4YSDhIBmFmf3cmZo
M+WFUsiuf6PSgfc1hlGoQQ==
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		checkForVaultStateContainer.setVisible(true);

		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+RHgSD/I+WCxxBNuiBWU5YfQoLvafDjoLU2liaAmKeAKRdDE6AK0n2
PpWvtGoiRxel1f0KnQ5l4cbf2ea1hFboNDvYvTj8cP4=
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		this.checkForVaultState();
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+hXazwkumUoSjI5P8J+2xfzlFOEjKTQlse0x7qmXOu45zyH+6KZ7MK
PW8TUVGxlRWkypu1l1fKV7gy4WuDHpHdiHYjkC78Lf8=
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private void checkForVaultState() {
		checkForVaultStateStatus.setText(localization.getString("additional.message.checkingVaultState"));
		checkForVaultStateIndicator.setVisible(true);
		Tasks.create(() -> {
			state = vaultState.check();
	    	switch (state) {
	        case NEW:
	        	Platform.runLater(() -> {
					this.checkForVaultStateStatus.setText(localization.getString("additional.message.initNewVault"));
				});
	        	listener.ifPresent(lstnr -> lstnr.didWelcome(state));
				break;
	        case RUN:
	        	Platform.runLater(() -> {
					this.checkForVaultStateStatus.setText(localization.getString("additional.message.existingVault"));
				});
	        	listener.ifPresent(lstnr -> lstnr.didWelcome(state));
				break;
	        case NOTFOUND:
	        	Platform.runLater(() -> {
					this.checkForVaultStateStatus.setText(localization.getString("additional.message.stateFileMissing"));
				});
				try {
				    Thread.sleep(5000);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
				this.checkForVaultState();
				break;
	        case ERROR:
	        	Platform.runLater(() -> {
					this.checkForVaultStateStatus.setText(localization.getString("additional.errorMessage.errCheckingStateFile"));
				});
	        	listener.ifPresent(lstnr -> lstnr.didWelcome(state));
				break;
	        case UNDEFINED:
	        	Platform.runLater(() -> {
					this.checkForVaultStateStatus.setText(localization.getString("additional.errorMessage.vaultStateCorrupt"));
				});
	        	listener.ifPresent(lstnr -> lstnr.didWelcome(state));
				break;
	        default:
	        	Platform.runLater(() -> {
					this.checkForVaultStateStatus.setText(localization.getString("additional.errorMessage.cannotDetermineVaultState"));
				});
	        	listener.ifPresent(lstnr -> lstnr.didWelcome(state));
				break;
			}
		}).runOnce(executor);
	}

	// ****************************************
	// Check for updates
	// ****************************************

	private boolean areUpdatesManagedExternally() {
		return Boolean.parseBoolean(System.getProperty("cryptomator.updatesManagedExternally", "false"));
	}

	private void askForUpdateCheck() {
		Tasks.create(() -> {}).onSuccess(() -> {
			Optional<ButtonType> result = buildYesNoDialog(
					localization.getString("welcome.askForUpdateCheck.dialog.title"),
					localization.getString("welcome.askForUpdateCheck.dialog.header"),
					localization.getString("welcome.askForUpdateCheck.dialog.content"),
					ButtonType.YES).showAndWait();
			if (result.isPresent()) {
				settings.askedForUpdateCheck().set(true);
				settings.checkForUpdates().set(result.get().equals(ButtonType.YES));
			}
			if (settings.checkForUpdates().get()) {
				this.checkForUpdates();
			}
		}).scheduleOnce(executor, 1, TimeUnit.SECONDS);
	}

	private void checkForUpdates() {
		checkForUpdatesStatus.setText(localization.getString("welcome.checkForUpdates.label.currentlyChecking"));
		checkForUpdatesIndicator.setVisible(true);
		Tasks.create(() -> {
			String userAgent = String.format("Cryptomator VersionChecker/%s %s %s (%s)", applicationVersion.orElse("SNAPSHOT"), SystemUtils.OS_NAME, SystemUtils.OS_VERSION, SystemUtils.OS_ARCH);
			URL url = URI.create("https://api.cryptomator.org/updates/latestVersion.json").toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.addRequestProperty("User-Agent", userAgent);
			conn.connect();
			try {
				if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
					return Optional.<byte[]>empty();
				}
				try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					in.transferTo(out);
					return Optional.of(out.toByteArray());
				}
			} finally {
				conn.disconnect();
			}
		}).onSuccess(response -> {
			response.ifPresent(bytes -> {
				Gson gson = new GsonBuilder().setLenient().create();
				String json = new String(bytes, StandardCharsets.UTF_8);
				Map<String, String> map = gson.fromJson(json, new TypeToken<Map<String, String>>() {
				}.getType());
				if (map != null) {
					this.compareVersions(map);
				}
			});
		}).onError(Exception.class, e -> {
			LOG.warn("Error checking for updates", e);
		}).andFinally(() -> {
			checkForUpdatesStatus.setText("");
			checkForUpdatesIndicator.setVisible(false);
		}).runOnce(executor);
	}

	private void compareVersions(final Map<String, String> latestVersions) {
		assert Platform.isFxApplicationThread();
		final String latestVersion;
		if (SystemUtils.IS_OS_MAC_OSX) {
			latestVersion = latestVersions.get("mac");
		} else if (SystemUtils.IS_OS_WINDOWS) {
			latestVersion = latestVersions.get("win");
		} else if (SystemUtils.IS_OS_LINUX) {
			latestVersion = latestVersions.get("linux");
		} else {
			// no version check possible on unsupported OS
			return;
		}
		final String currentVersion = applicationVersion.orElse(null);
		LOG.info("Current version: {}, lastest version: {}", currentVersion, latestVersion);
		if (currentVersion != null && semVerComparator.compare(currentVersion, latestVersion) < 0) {
			final String msg = String.format(localization.getString("welcome.newVersionMessage"), latestVersion, currentVersion);
			this.updateLink.setText(msg);
			this.updateLink.setVisible(true);
			this.updateLink.setDisable(false);
		}
	}

	@FXML
	public void didClickUpdateLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/");
	}

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/uXr/BOrEGJwL7nHovCt2LXU91fKQK3+TeObdbBAQd0JVyvKFHrEuI
QzxKsx1pebdS74zzqmM1izFwo+qlAM9qyi8q4AQIODI=
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	public void setListener(WelcomeListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	@FunctionalInterface
	interface WelcomeListener {
		void didWelcome(VaultState.State state);
	}

}
