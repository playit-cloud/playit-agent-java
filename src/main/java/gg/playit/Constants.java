package gg.playit;

import gg.playit.api.models.AgentVersion;
import gg.playit.api.models.Platform;
import gg.playit.api.models.PlayitAgentVersion;

public class Constants {
    public static PlayitAgentVersion version;

    static {
        AgentVersion agentVersion = new AgentVersion();
        agentVersion.platform(Platform.MINECRAFT_PLUGIN);
        agentVersion.setVersion("java:0.15.0");

        version = new PlayitAgentVersion();
        version.version(agentVersion);
        version.detailsWebsite("https://github.com/playit-cloud/playit-agent-java");
        version.official(true);
    }
}
