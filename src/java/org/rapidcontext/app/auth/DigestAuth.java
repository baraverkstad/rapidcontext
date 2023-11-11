package org.rapidcontext.app.auth;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.DateUtils;
import org.rapidcontext.app.model.Context;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.BinaryUtil;

public class DigestAuth {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(DigestAuth.class.getName());

    public void challenge(Request request) {
        String nonce = String.valueOf(System.currentTimeMillis());
//        request.sendAuth("Digest realm=\"" + User.DEFAULT_REALM + "\", qop=\"auth\", " +
//                         "algorithm=MD5, nonce=\"" + nonce + "\"");
    }

    /**
     * Processes a request digest authentication.
     *
     * @param request        the request to process
     * @param scheme         the authentication scheme
     * @param data           the authentication data
     *
     * @return true if the authentication was processed, or
     *         false otherwise
     *
     * @throws Exception if the user authentication failed
     */
    public User authResponse(Request request, String scheme, Dict data)
    throws Exception {
        if (!scheme.equalsIgnoreCase("Digest")) {
            return null;
        } else if (!User.DEFAULT_REALM.equals(data.get("realm"))) {
            LOG.warning("unsupported authentication realm: " + data.get("realm"));
            return null;
        }

        String uri = data.getString("uri", request.getAbsolutePath());
        data.set("extra", BinaryUtil.hashMD5(request.getMethod() + ":" + uri));
        return auth(data);
    }

    public User auth(Dict data) throws Exception {
        String userId = data.getString("username", "");
        User user = User.find(Context.get().getStorage(), userId);
        if (user == null) {
            String msg = "user " + userId + " does not exist";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        }
        String nonce = data.getString("nonce", "");
        verifyNonce(nonce);
        StringBuilder buf = new StringBuilder();
        buf.append(user.passwordHash());
        buf.append(":");
        buf.append(nonce);
        if (data.containsKey("nc")) {
            buf.append(":" + data.getString("nc", ""));
        }
        if (data.containsKey("cnonce")) {
            buf.append(":" + data.getString("cnonce", ""));
        }
        if (data.containsKey("qop")) {
            buf.append(":" + data.getString("qop", ""));
        }
        if (data.containsKey("extra")) {
            buf.append(":" + data.getString("extra", ""));
        }
        String response = data.getString("response", "");
        try {
            String challenge = BinaryUtil.hashMD5(buf.toString());
            if (user.passwordHash().length() > 0 && !challenge.equals(response)) {
                String msg = "invalid password for user " + userId;
                LOG.info("failed authentication: " + msg +
                         ", expected: " + challenge + ", received: " + response);
                throw new SecurityException(msg);
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            String msg = "invalid environment, MD5 not supported";
            LOG.log(Level.SEVERE, msg, e);
            throw new SecurityException(msg, e);
        }
        SecurityContext.auth(userId);
        return user;
    }

    /**
     * Verifies that the specified nonce is sufficiently recently
     * generated to be acceptable.
     *
     * @param nonce          the nonce to check
     *
     * @throws SecurityException if the nonce was invalid
     */
    private void verifyNonce(String nonce) throws SecurityException {
        try {
            long since = System.currentTimeMillis() - Long.parseLong(nonce);
            if (since > DateUtils.MILLIS_PER_MINUTE * 10) {
                LOG.info("stale authentication one-off number");
                throw new SecurityException("stale authentication one-off number");
            }
        } catch (NumberFormatException e) {
            LOG.info("invalid authentication one-off number");
            throw new SecurityException("invalid authentication one-off number");
        }
    }
}
