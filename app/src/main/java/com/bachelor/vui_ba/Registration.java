package com.bachelor.vui_ba;

import com.nuance.speechanywhere.Session;

public class Registration {
    // Convenience defines used for opening the Session instance and pass it the licensing
    // information needed. Your partner GUID and organization token will be made available to you via the
    // Nuance order desk.
    private static final String applicationName = "Android Sample";

    // THIS IS CUSTOMER SPECIFIC - MUST *NOT* BE HARD-CODED!
    // THIS IS EQUIVALENT TO A LICENSE KEY - MUST BE KEPT SECRET FROM UNAUTHORIZED USERS!
    // Make it configurable via user settings or download it from your server if you have a client-server app.
    private String organizationToken = "B7ECC65F-1790-42A9-89FF-606D72DA57ED";

    // It is ok to hard-code the partner GUID - should be "hidden" within your application,
    // usually will not need to be changed.
    private String partnerToken = "a76110e5-2bad-4b54-9abf-8a3b15619e66";

    private static final String userId = "bachelorthesis";

    public void openNuanceSession() {
        // Open the session by supplying the given user name, the proper credentials and the application's name
        Session.getSharedSession().open(userId, organizationToken, partnerToken, applicationName);
    }
}
