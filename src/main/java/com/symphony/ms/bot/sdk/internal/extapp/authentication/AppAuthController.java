package com.symphony.ms.bot.sdk.internal.extapp.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.symphony.ms.bot.sdk.internal.symphony.ConfigClient;
import com.symphony.ms.bot.sdk.internal.symphony.ExtensionAppAuthClient;
import com.symphony.ms.bot.sdk.internal.symphony.exception.AppAuthenticateException;
import com.symphony.ms.bot.sdk.internal.symphony.exception.SymphonyClientException;
import com.symphony.ms.bot.sdk.internal.symphony.model.AuthenticateResponse;

/**
 * Extension App authentication controller
 * Exposes endpoints through which an extension app could authenticate itself
 * to Symphony.
 *
 * @author Marcus Secato
 *
 */
@RestController
@RequestMapping(value = "/application")
public class AppAuthController {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppAuthController.class);

  private ExtensionAppAuthClient extAppAuthClient;
  private ConfigClient configClient;

  public AppAuthController(ExtensionAppAuthClient extAppAuthClient, ConfigClient configClient) {
    this.extAppAuthClient = extAppAuthClient;
    this.configClient = configClient;
  }

  @PostMapping("authenticate")
  public ResponseEntity authenticate(@RequestBody AppInfo appInfo) {
    LOGGER.debug("App auth step 1: Initializing extension app authentication");

    if (appInfo.getAppId() == null) {
      LOGGER.debug("Rejecting authenticate request: null app ID received");
      return ResponseEntity.badRequest().build();
    }

    if (!appInfo.getAppId().equals(configClient.getExtAppId())) {
      LOGGER.debug("Rejecting authenticate request: app ID does not match");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      AuthenticateResponse authenticateResponse =
          extAppAuthClient.appAuthenticate(appInfo.getAppId());
      return ResponseEntity.ok(authenticateResponse);
    } catch (AppAuthenticateException aae) {
      LOGGER.info("Authentication process failed");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (SymphonyClientException sce) {
      LOGGER.error("Error initializing extension app authentication flow\n{}", sce);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PostMapping("tokens/validate")
  public ResponseEntity validateTokens(@RequestBody AppToken appToken) {
    LOGGER.debug("App auth step 2: Validating tokens");
    if (extAppAuthClient.validateTokens(
        appToken.getAppToken(), appToken.getSymphonyToken())) {
      return ResponseEntity.ok().build();
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  @PostMapping("jwt/validate")
  public ResponseEntity validateJwt(@RequestBody JwtInfo jwtInfo) {
    LOGGER.debug("App auth step 3: Validating JWT");
    try {
      return ResponseEntity.ok(
          extAppAuthClient.verifyJWT(jwtInfo.getJwt()));
    } catch (AppAuthenticateException aae) {
      LOGGER.error("Error validating JWT");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

}
