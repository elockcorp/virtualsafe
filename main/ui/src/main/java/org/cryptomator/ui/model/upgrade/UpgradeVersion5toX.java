/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model.upgrade;

import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.cryptofs.migration.Migrators;
import org.cryptomator.cryptofs.migration.api.NoApplicableMigratorException;
import org.cryptomator.cryptolib.Cryptors;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+h7AYSkavkuHDG/Zku4BcTjpFvFynTzx5W8X2ZmFZY1Iom/fLMGaUd
yhVvfg3a2JYgZyQ7eUp5eQ==
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
import static org.cryptomator.ui.util.Constants.MASTERKEY_FILENAME;

@FxApplicationScoped
class UpgradeVersion5toX extends UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion5toX.class);

	@Inject
	public UpgradeVersion5toX(Localization localization) {
		super(Cryptors.version1(UpgradeStrategy.strongSecureRandom()), localization, 5, Integer.MAX_VALUE);
	}

	@Override
	public String getTitle(Vault vault) {
		return localization.getString("upgrade.version5toX.title");
	}

	@Override
	public String getMessage(Vault vault) {
		return localization.getString("upgrade.version5toX.msg");
	}

	@Override
	public void upgrade(Vault vault, CharSequence passphrase) throws UpgradeFailedException {
		try {
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1+sE5NLyKB5jtoqnzOZDTujD4wSm8JSQfSQoltRVJ/tyPSaDYSdJ0eK
kb/qRPK0lp4q+Y56r9yASfvamDsXxpIF9y4SZ/NeIklMigqWbt8WwbyuXMAkZ1WZ
zhoIs/wfnhpCg48E+AFacdbqLfYF6jBpN/UKnOm9Ws9wm2WG+Q99/BIhE6f5s9C+
ziau9+xzkfYiuA5CFzQTyA==
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			Migrators.get().migrate(vault.getPath(), MASTERKEY_FILENAME, passphrase);
		} catch (InvalidPassphraseException e) {
			throw new UpgradeFailedException(localization.getString("unlock.errorMessage.wrongPassword"));
		} catch (NoApplicableMigratorException | IOException e) {
			LOG.warn("Upgrade failed.", e);
			throw new UpgradeFailedException("Upgrade failed. Details in log message.");
		}
	}

	@Override
	protected void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException {
		// called by #upgrade(Vault, CharSequence), which got overwritten.
		throw new AssertionError("Method can not be called.");
	}

	@Override
	public boolean isApplicable(Vault vault) {
		try {
			/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX1/jO0NyJf1h24AUJMLct85VFktNL9wW8Dsx0WLNN3TLzheFb0f9mqrV
h5YRl8e4GtBq2VXckzFxXqI9JMZlzjfZ4UOJyDw2NHfIb/zSmvnBbtM/OKHnih4o
WaTXmOf6C//hDKZQoera8S4P1uceaaGLWTS0gP9zpgdX4sfyeHAt04PBV2eXovFn
wQDK+xvUX2eIAtSn/3jzsw==
			###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
			return Migrators.get().needsMigration(vault.getPath(), MASTERKEY_FILENAME);
		} catch (IOException e) {
			LOG.warn("Could not determine, whether upgrade is applicable.", e);
			return false;
		}
	}

}
