package com.sarabblog.googlesmartlockforandroid;

import com.google.android.gms.auth.api.credentials.Credential;

class Util {

    private static String[][] validCredentials = {
            {"testusername1", "password1"},
            {"testusername2", "password2"}
    };

    /**
     * Check whether or not given username and password pair exist
     */
    private static boolean isValidCredential(String username, String password) {
        for (String[] credential :
                validCredentials) {
            if (credential[0].equals(username) && credential[1].equals(password))
                return true;
        }
        return false;
    }

    static boolean isValidCredential(Credential credential) {
        String username = credential.getId();
        String password = credential.getPassword();
        return isValidCredential(username, password);
    }
}