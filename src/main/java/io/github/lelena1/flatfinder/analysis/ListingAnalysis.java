package io.github.lelena1.flatfinder.analysis;

/** Что удалось понять об объявлении: из структурированных полей и из текста описания. */
public record ListingAnalysis(
        LayoutType layout,
        Tri balcony,
        Tri balconyFromKitchen,
        Tri storage,
        Tri readyFinish,
        Tri furniture
) {
}
