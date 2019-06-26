/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX18cRPHqgtUffkq+z7XGXnlvAXKQzZxCMVE8bP53Z25i0RxCYSuraibS
+j9G5yYw47wEGlOoJLuGlRNWSa0eKuT90oBbF4pnf30=
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */

/*
 * Copyright 2017 www.elock.com.my
 */

package org.cryptomator.ui.util;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.NoSuchFileException;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.cryptomator.ui.util.Constants.STATE_FILENAME;

@Singleton
public final class VaultState {

    private static final Logger LOG = LoggerFactory.getLogger(VaultState.class);

	@Inject
    public VaultState() {
    }

	public enum State {
		NEW,
		RUN,
		NOTFOUND,
		ERROR,
		UNDEFINED
	}

    private static State state = State.UNDEFINED;

    public State check() {
		try {
			final Path statefile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + STATE_FILENAME);
			String vaultStateStr = new String(Files.readAllBytes(statefile)).trim();
	    	switch (vaultStateStr) {
            case "NEW":
            	state = State.NEW;
				break;
            case "RUN":
            	state = State.RUN;
				break;
            default:
            	state = State.UNDEFINED;
				break;
			}
		} catch (NoSuchFileException | FileNotFoundException e) {
			state = State.NOTFOUND;
		} catch (IOException e) {
			state = State.ERROR;
			LOG.error("Error checking vault state file.", e);
		}
    	return this.state;
    }

    public State setState(State newstate) {
		try {
			final Path statefile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "VSYNC" + System.getProperty("file.separator") + STATE_FILENAME);
			OutputStream out = Files.newOutputStream(statefile, StandardOpenOption.TRUNCATE_EXISTING);
			Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
	    	switch (newstate) {
            case NEW:
				writer.write("NEW");
				LOG.info("State file set to NEW.");
				break;
            case RUN:
				writer.write("RUN");
				LOG.info("State file set to RUN.");
				break;
            default:
				break;
			}
			writer.close();
			out.close();
		} catch (IOException e) {
			LOG.error("Error writing vault state file.", e);
			state = this.check();
		}
    	return this.state;
    }
}
