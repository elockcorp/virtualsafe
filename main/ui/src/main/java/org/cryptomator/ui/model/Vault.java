/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.LazyInitializer;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProperties.FileSystemFlags;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.model.VaultModule.PerVault;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19TCsOZzk4HanCAODSJeOUv+TTsko/7w2W1DRPvskmbuDzXJgkmAq7L
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
import static org.cryptomator.ui.util.Constants.MASTERKEY_FILENAME;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.DirectoryStream;
import mslinks.ShellLink;

@PerVault
public class Vault {

	public static final Predicate<Vault> NOT_LOCKED = hasState(State.LOCKED).negate();
	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);
	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/nI2BISjlaNnGv0qpI8q7OFxHUywNntCl1bra38Wpx9+R1qW+2rvz0
JRKa6Oq7d1LDwof7ubL9zVwLvOWRL1Tj3QjAC1MzYdd0MSkXODEKeRB6rZ3ue92U
yfQjtOH9mzAODGN6JFZdXHfsgjfwbbB3Kee88iyX9TsYXOnN78azKyOLGS/e8qPn
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */

	private final VaultSettings vaultSettings;
	private final Provider<Volume> volumeProvider;
	private final AtomicReference<CryptoFileSystem> cryptoFileSystem = new AtomicReference<>();
	private final ObjectProperty<State> state = new SimpleObjectProperty<State>(State.LOCKED);

	private Volume volume;

	public enum State {
		LOCKED, PROCESSING, UNLOCKED
	}

	@Inject
	Vault(VaultSettings vaultSettings, Provider<Volume> volumeProvider) {
		this.vaultSettings = vaultSettings;
		this.volumeProvider = volumeProvider;
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem getCryptoFileSystem(CharSequence passphrase) throws NoSuchFileException, IOException, InvalidPassphraseException, CryptoException {
		return LazyInitializer.initializeLazily(cryptoFileSystem, () -> unlockCryptoFileSystem(passphrase), IOException.class);
	}

	private CryptoFileSystem unlockCryptoFileSystem(CharSequence passphrase) throws NoSuchFileException, IOException, InvalidPassphraseException, CryptoException {
		List<FileSystemFlags> flags = new ArrayList<>();
		if (vaultSettings.usesReadOnlyMode().get()) {
			flags.add(FileSystemFlags.READONLY);
		}
		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
				.withPassphrase(passphrase) //
				.withFlags(flags) //
				.withMasterkeyFilename(MASTERKEY_FILENAME) //
				.build();
		return CryptoFileSystemProvider.newFileSystem(getPath(), fsProps);
	}

	public void create(CharSequence passphrase) throws IOException {
		if (!isValidVaultDirectory()) {
			CryptoFileSystemProvider.initialize(getPath(), MASTERKEY_FILENAME, passphrase);
		} else {
			throw new FileAlreadyExistsException(getPath().toString());
		}
	}

	public void changePassphrase(CharSequence oldPassphrase, CharSequence newPassphrase) throws IOException, InvalidPassphraseException {
		CryptoFileSystemProvider.changePassphrase(getPath(), MASTERKEY_FILENAME, oldPassphrase, newPassphrase);
	}

	public synchronized void unlock(CharSequence passphrase) throws CryptoException, IOException, Volume.VolumeException {
		Platform.runLater(() -> state.set(State.PROCESSING));
		try {
			if (vaultSettings.usesIndividualMountPath().get() && Strings.isNullOrEmpty(vaultSettings.individualMountPath().get())) {
				throw new NotDirectoryException("");
			}
			CryptoFileSystem fs = getCryptoFileSystem(passphrase);
			volume = volumeProvider.get();
			volume.mount(fs);
			Platform.runLater(() -> {
				state.set(State.UNLOCKED);
			});

			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/cvfDcFJbomMYV4iyDkA6aWoDlWw7XiXtXxIizXrNe3Ob1Mil9VbIs
iSmtM2+nawtIymQS7a029EnUjP7kRgWXYLQwH/+NJRc=
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			if (SystemUtils.IS_OS_MAC_OSX) {
				final Path icns = Paths.get("../Resources/VolumeIcon.icns");
				final Path icns_res_fork = Paths.get("../Resources/_.VolumeIcon.icns");
				final Path folder_res_fork = Paths.get("../Resources/_.");

				/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+xkMMWcArAl1FA692RU9Oak1N6EgvHuFxTz6LIlG7iXNS18OhAxKNv
ZwKWR8e7fhQQekJ3oHz/RTlu7Agu3q8bGdP84AyO9znAdnxbs+I0kR7+7yfcbgWM
51sTqzPfrY1e7fSLnngICup0KrFTlZ5UaKgQwPiSYyAD9NqZdCd8ROu0i8vozpJz
YECzQwWiTpKHaqX6+wLI2x12zwDgpZzC4+QnqeIdLzoOpiTNZSfuYLgfTcTzsdxO
m87w89Jqw+BbQaGHDNB8Xz7tnwIqQxhfV23xsxCqU6w=
				###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
				List<Path> possible_mount_points = new ArrayList<>();
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("/Volumes"), this.getMountName() + "*")) {
					for (Path entry: stream) {
						possible_mount_points.add(entry);
					}
				} catch (Exception e) {
					// I/O error encounted during the iteration, the cause is an IOException
					LOG.error("Fail to iterate mount volumes.", e);
				}

				for (Path entry: possible_mount_points) {
					try {
						Files.copy(icns, Paths.get(entry.toString() + "/.VolumeIcon.icns"), REPLACE_EXISTING);
						Files.copy(icns_res_fork, Paths.get(entry.toString() + "/._.VolumeIcon.icns"), REPLACE_EXISTING);
						Files.copy(folder_res_fork, Paths.get(entry.toString() + "/._."), REPLACE_EXISTING);
					} catch (AccessDeniedException se) {
					} catch (Exception e) {
						LOG.error("Fail to set mounted vault icon.", e);
					}
				}
			}

			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/+0UglEvUe6Xwsxc2bDUj9cLOFcPoXqoTYx8fCWHoEmjx6YLa7fhk7
SLV+FG0VQr6l6Mw9GxD04UCNSadVmO+uxWFYry79xQJUqdnMcD5KZp25hIO2DINF
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			if (SystemUtils.IS_OS_WINDOWS) {
				final Path iconPath = Paths.get(SystemUtils.USER_DIR + "\\..\\VirtualSAFE.ico").normalize();
				final Path shortcutLinkPath = Paths.get(SystemUtils.USER_HOME + "\\Desktop\\My VirtualSAFE.lnk");
				final Path mountPath = Paths.get(this.getWinDriveLetter() + ":\\");
				
				LOG.debug("iconPath: " + iconPath.toString());
				LOG.debug("shortcutLinkPath: " + shortcutLinkPath.toString());
				LOG.debug("mountPath: " + mountPath.toString());
				
				if (shortcutLinkPath != null && !Files.exists(shortcutLinkPath)) {
					if (mountPath != null && Files.isDirectory(mountPath))
					{
						if (iconPath != null) {
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18o/e0gLj7q22fRFyrlMZHuf/2sKtZ3CCFJGu+LzCWHVG7tB7n2A8KR
KvNGikh64RDsSECaFaU8rajPmFywODZ+KxIcBoSnsj4=
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							try {
								ShellLink sl = ShellLink.createLink(mountPath.toString())
									.setWorkingDir("..")
									.setIconLocation(iconPath.toString());
								sl.getConsoleData()
									.setFont(mslinks.extra.ConsoleData.Font.Consolas)
									.setFontSize(24)
									.setTextColor(5);
								sl.saveTo(shortcutLinkPath.toString());
								LOG.debug("sl.getWorkingDir: " + sl.getWorkingDir());
								LOG.debug("sl.resolveTarget: " + sl.resolveTarget());
							} catch (Exception e) {
								LOG.error("Fail to create Desktop shortcut and set its icon.", e);
							}
						} else {
							/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+b5UzqsmNKeuW1K1+Tbst+cyGM2xKmzYlVDtB4msC6IWaMoCqa9LKw
2Q+2jMoAbSgNUOkyRhNae6feAhha6T6f8bh3rWYwSVE=
							###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
							LOG.error("Cannot set icon for Desktop shortcut. Missing icon file.");
							try {
								ShellLink sl = ShellLink.createLink(mountPath.toString(), shortcutLinkPath.toString());
							} catch (Exception e) {
								LOG.error("Fail to create Desktop shortcut.", e);
							}
						}
					}
				}						
			}
		} catch (Exception e) {
			Platform.runLater(() -> state.set(State.LOCKED));
			throw e;
		}
	}

	public synchronized void lock(boolean forced) throws Volume.VolumeException {
		Platform.runLater(() -> {
			state.set(State.PROCESSING);
		});
		if (forced && volume.supportsForcedUnmount()) {
			volume.unmountForced();
		} else {
			volume.unmount();
		}
		CryptoFileSystem fs = cryptoFileSystem.getAndSet(null);
		if (fs != null) {
			try {
				fs.close();
			} catch (IOException e) {
				LOG.error("Error closing file system.", e);
			}
		}
		Platform.runLater(() -> {
			state.set(State.LOCKED);
		});
	}

	/**
	 * Ejects any mounted drives and locks this vault. no-op if this vault is currently locked.
	 */
	public void prepareForShutdown() {
		try {
			lock(false);
		} catch (Volume.VolumeException e) {
			if (volume.supportsForcedUnmount()) {
				try {
					lock(true);
				} catch (Volume.VolumeException e1) {
					LOG.warn("Failed to force lock vault.", e1);
				}
			} else {
				LOG.warn("Failed to gracefully lock vault.", e);
			}
		}
	}

	public void reveal() throws Volume.VolumeException {
		volume.reveal();
	}

	// ******************************************************************************
	// Getter/Setter
	// *******************************************************************************/

	public State getState() {
		return state.get();
	}

	public ReadOnlyObjectProperty<State> stateProperty() {
		return state;
	}

	public static Predicate<Vault> hasState(State state) {
		return vault -> {
			return vault.getState() == state;
		};
	}

	public Observable[] observables() {
		return new Observable[]{state};
	}

	public VaultSettings getVaultSettings() {
		return vaultSettings;
	}

	public Path getPath() {
		return vaultSettings.path().getValue();
	}

	public Binding<String> displayablePath() {
		Path homeDir = Paths.get(SystemUtils.USER_HOME);
		return EasyBind.map(vaultSettings.path(), p -> {
			if (p.startsWith(homeDir)) {
				Path relativePath = homeDir.relativize(p);
				String homePrefix = SystemUtils.IS_OS_WINDOWS ? "~\\" : "~/";
				return homePrefix + relativePath.toString();
			} else {
				return p.toString();
			}
		});
	}

	/**
	 * @return Directory name without preceeding path components and file extension
	 */
	public Binding<String> name() {
		return EasyBind.map(vaultSettings.path(), Path::getFileName).map(Path::toString);
	}

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18hfPYaeksMrf0hpBU/7iEV+YUJGrvZmMGqMgHGIDMt+ALe48vu2X2e
iGZAMwuUHUIuDiXaCHY231NjJqhKJRGZbqAlP0b0QJ4=
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	public Binding<String> mountName() {
		return EasyBind.map(vaultSettings.mountName(), p -> {
			return p.toString();
		});
	}

	public boolean doesVaultDirectoryExist() {
		return Files.isDirectory(getPath());
	}

	public boolean isValidVaultDirectory() {
		return CryptoFileSystemProvider.containsVault(getPath(), MASTERKEY_FILENAME);
	}

	public long pollBytesRead() {
		CryptoFileSystem fs = cryptoFileSystem.get();
		if (fs != null) {
			return fs.getStats().pollBytesRead();
		} else {
			return 0l;
		}
	}

	public long pollBytesWritten() {
		CryptoFileSystem fs = cryptoFileSystem.get();
		if (fs != null) {
			return fs.getStats().pollBytesWritten();
		} else {
			return 0l;
		}
	}

	public String getMountName() {
		return vaultSettings.mountName().get();
	}

	public String getCustomMountPath() {
		return vaultSettings.individualMountPath().getValueSafe();
	}

	public void setCustomMountPath(String mountPath) {
		vaultSettings.individualMountPath().set(mountPath);
	}

	public void setMountName(String mountName) throws IllegalArgumentException {
		if (StringUtils.isBlank(mountName)) {
			throw new IllegalArgumentException("mount name is empty");
		} else {
			vaultSettings.mountName().set(VaultSettings.normalizeMountName(mountName));
		}
	}

	public Character getWinDriveLetter() {
		if (vaultSettings.winDriveLetter().get() == null) {
			return null;
		} else {
			return vaultSettings.winDriveLetter().get().charAt(0);
		}
	}

	public void setWinDriveLetter(Character winDriveLetter) {
		if (winDriveLetter == null) {
			vaultSettings.winDriveLetter().set(null);
		} else {
			vaultSettings.winDriveLetter().set(String.valueOf(winDriveLetter));
		}
	}

	public String getId() {
		return vaultSettings.getId();
	}

	// ******************************************************************************
	// Hashcode / Equals
	// *******************************************************************************/

	@Override
	public int hashCode() {
		return Objects.hash(vaultSettings);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vault && obj.getClass().equals(this.getClass())) {
			final Vault other = (Vault) obj;
			return Objects.equals(this.vaultSettings, other.vaultSettings);
		} else {
			return false;
		}
	}

	public boolean supportsForcedUnmount() {
		return volume.supportsForcedUnmount();
	}
}
