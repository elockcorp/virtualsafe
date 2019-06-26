package org.cryptomator.ui.model;


import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.frontend.webdav.mount.MountParams;
import org.cryptomator.frontend.webdav.mount.Mounter;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;

import javax.inject.Inject;
import javax.inject.Provider;

import java.net.InetAddress;
import java.net.UnknownHostException;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18ni8DEq427bHxDO0YeEgCZIOSONFMLmPIWm46W2vIGZAPzrHQDUBLj
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
import org.apache.commons.lang3.SystemUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDavVolume implements Volume {

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18QBw3lccGkS2Hiq+ST3akEiHDRkduHGw7dxN4GUkIly1hisf5ecnf/
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private static final Logger LOG = LoggerFactory.getLogger(WebDavVolume.class);

	/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/2Ahj+vpcn2Owq63qt8VgnEvkCaxnk5mlQkmFhanZ+Gce+n+2PntEz
4rOvEHieWBt5C7KPHpu6867HQpYBlihQ6//BjfUCNi2dm4SecrPhFKIHMDJGFc4P
w+UE2OTKlCq3Gn2hIh5ySeVduIhupCbfdYEo3OeacoewHM/jgQgcyBXqEDxaqA8Z
	###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
	private static final String LOCALHOST_ALIAS = "virtualsafe-vault";

	private final Provider<WebDavServer> serverProvider;
	private final VaultSettings vaultSettings;
	private final Settings settings;

	private WebDavServer server;
	private WebDavServletController servlet;
	private Mounter.Mount mount;

	@Inject
	public WebDavVolume(Provider<WebDavServer> serverProvider, VaultSettings vaultSettings, Settings settings) {
		this.serverProvider = serverProvider;
		this.vaultSettings = vaultSettings;
		this.settings = settings;
	}

	@Override
	public void mount(CryptoFileSystem fs) throws VolumeException {
		if (server == null) {
			server = serverProvider.get();
		}
		if (!server.isRunning()) {
			server.start();
		}
		servlet = server.createWebDavServlet(fs.getPath("/"), vaultSettings.getId() + "/" + vaultSettings.mountName().get());
		servlet.start();
		mount();
	}

	private void mount() throws VolumeException {
		if (servlet == null) {
			throw new IllegalStateException("Mounting requires unlocked WebDAV servlet.");
		}
		MountParams mountParams = MountParams.create() //
				.withWindowsDriveLetter(vaultSettings.winDriveLetter().get()) //
				.withPreferredGvfsScheme(settings.preferredGvfsScheme().get())//
				.withWebdavHostname(getLocalhostAliasOrNull()) //
				.build();
		try {
			this.mount = servlet.mount(mountParams); // might block this thread for a while
		} catch (Mounter.CommandFailedException e) {
			e.printStackTrace();
			throw new VolumeException(e);
		}
	}

	@Override
	public void reveal() throws VolumeException {
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19qz+CXqdUyEEkjpyHHQbvlIeToTWJDR9KEm6BeuWJHbRCKn1FLZ4KE
4EGsNJY5thVfD9uGwJPkdEE7lVT/daf4t9Exilm15X+LhHYF7qrjAPKc8+LKxEcw
7TKvNxYoJ6vMxF/0afqKo300jS06pcEd6abx46EyCvy/Gkh2W5C+c9zCxyunGsXM
pDornkhqgVjv2QMoKN2OG+COnnfiiNxR1K5DCWO1wA5Ja4ZcoDpzCxm3sGyZOEiM
SP5v1XExYnfUzV+uwYXLtVodu973vkHcsVf8byuxoA4jQdABATMfJCy4zf3tUlhp
9PSp3SJhmAuSieuyqhXIujQZ6Fu5qF6t12wgJto7cDAsdGcBtQL1cROEeV+2Jy0I
ksVbSPBaytEYyEGDdrXYd6WBZ6TlhqX4rw8iwHy3N8cfFcUbD+iwRosOVK0kOdIz
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		int count = 0;
		int maxTries = 2;
		while(true) {
			try {
				mount.reveal();
				break;
			} catch (Mounter.CommandFailedException e) {
				e.printStackTrace();
				if (++count == maxTries) throw new VolumeException(e);
			}
		}
	}

	@Override
	public synchronized void unmount() throws VolumeException {
		try {
			mount.unmount();
		} catch (Mounter.CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanup();
	}

	@Override
	public synchronized void unmountForced() throws VolumeException {
		try {
			mount.forced().orElseThrow(IllegalStateException::new).unmount();
		} catch (Mounter.CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanup();
	}

	private String getLocalhostAliasOrNull() {
		try {
			InetAddress alias = InetAddress.getByName(LOCALHOST_ALIAS);
			if (alias.getHostAddress().equals("127.0.0.1")) {
				return LOCALHOST_ALIAS;
			} else {
				return null;
			}
		} catch (UnknownHostException e) {
			return null;
		}
	}

	private void cleanup() {
		if (servlet != null) {
			servlet.stop();
		}
		/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX189Fl6xO+MXt+GfgVGAYZJSjLEcik/G39DYj7ZWjioU0UvAWiY3zK3O
h/4ub6UVhqKYp2GzPXxxQNKaKdivHW+N+ARmmAQKbNUTiq0JxOuEr7JZrS+bT6fr
		###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
		if (SystemUtils.IS_OS_WINDOWS) {
			try {
				final Path shortcutLinkPath = Paths.get(SystemUtils.USER_HOME + "\\Desktop\\My VirtualSAFE.lnk");
				Files.deleteIfExists(shortcutLinkPath);
			} catch (SecurityException | IOException e) {
				LOG.warn("Unable to delete desktop shortcut:", e);
			}
		}
	}

	@Override
	public boolean isSupported() {
		return WebDavVolume.isSupportedStatic();
	}

	@Override
	public boolean supportsForcedUnmount() {
		return mount != null && mount.forced().isPresent();
	}


	public static boolean isSupportedStatic() {
		return true;
	}
}
