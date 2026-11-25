package com.kisansetu.claim;

import java.util.List;

public record PagedClaims(List<ClaimView> items, int page, int size, long total) {
}
