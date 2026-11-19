package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.platform.Platform;

/**
 * What differs between the loaders on the client only, kept apart from {@link Platform}, which a dedicated server loads too. Each
 * loader project provides one implementation, listed in its {@code META-INF/services}.
 */
public interface ClientPlatform {
    ClientPlatform INSTANCE = Platform.load(ClientPlatform.class);
}
