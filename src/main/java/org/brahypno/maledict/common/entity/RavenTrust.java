package org.brahypno.maledict.common.entity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** A bred chick trusts the players who fed its parents, rather than inheriting ownership. */
public final class RavenTrust {
    public static List<UUID> fromBreeding(UUID firstFeeder, UUID secondFeeder) {
        return Stream.of(firstFeeder, secondFeeder).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private RavenTrust() {
    }
}
