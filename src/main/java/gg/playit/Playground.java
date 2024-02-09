package gg.playit;

import gg.playit.api.DefaultPlayitAPI;
import gg.playit.api.models.AgentRunData;

public class Playground {
    public static void main(String[] args) throws Exception {
        DefaultPlayitAPI api = new DefaultPlayitAPI();
        api.getApiClient().setAgentKey("");
        AgentRunData data = api.agentsRundataPost().getData();
    }
}
