package org.apache.bigtop.manager.stack.spi;

import org.apache.bigtop.manager.common.message.type.CommandMessage;
import org.apache.bigtop.manager.common.utils.shell.ShellResult;
import org.apache.commons.lang3.StringUtils;

public interface Script extends SPIIdentify {

    ShellResult install(CommandMessage commandMessage);

    ShellResult configuration(CommandMessage commandMessage);

    default ShellResult start(CommandMessage commandMessage) {
        return null;
    }

    default ShellResult stop(CommandMessage commandMessage) {
        return null;
    }

    default ShellResult restart(CommandMessage commandMessage) {
        ShellResult shellResult = stop(commandMessage);
        if (shellResult.getExitCode() != 0) {
            return shellResult;
        }
        ShellResult shellResult1 = start(commandMessage);
        if (shellResult1.getExitCode() != 0) {
            return shellResult1;
        }

        return new ShellResult(0,
                StringUtils.join(shellResult.getOutput(), shellResult1.getOutput()),
                StringUtils.join(shellResult.getErrMsg(), shellResult1.getErrMsg()));
    }

    ShellResult status(CommandMessage commandMessage);
}
