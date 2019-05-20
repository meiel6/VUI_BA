package com.bachelor.vui_ba;

import com.nuance.speechanywhere.Session;

/**
 * This Class represents the collection of Registration-Strings for using the Dragon Medical SpeechKit
 * and opens a connection (Session) to the Speech-Recognition utilities.
 */
public class Registration {
    private static final String applicationName = "Android Sample";
    private String organizationToken = "B7ECC65F-1790-42A9-89FF-606D72DA57ED";
    private String partnerToken = "a76110e5-2bad-4b54-9abf-8a3b15619e66";

    private static final String userId = "bachelorarbeit";

    /**
     * Opens a session to the Dragon Medcial Speech Kit functionality.
     */
    public void openNuanceSession() {
        Session.getSharedSession().open(userId, organizationToken, partnerToken, applicationName);
    }
}
