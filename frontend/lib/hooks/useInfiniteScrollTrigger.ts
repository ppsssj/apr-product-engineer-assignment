"use client";

import { useEffect, useRef } from "react";

type Options = {
  // Observe only while another page may be requested.
  enabled: boolean;
  onIntersect: () => void;
  rootMargin?: string;
};

// IntersectionObserver sentinel (ADR-010). The observer is recreated whenever
// `enabled` flips back to true, and a new observer reports the sentinel's
// current state immediately, so a sentinel that is still visible after a page
// resolves requests the next page without waiting for another scroll.
export function useInfiniteScrollTrigger<T extends Element>({
  enabled,
  onIntersect,
  rootMargin = "0px 0px 200px 0px",
}: Options) {
  const sentinelRef = useRef<T | null>(null);
  const onIntersectRef = useRef(onIntersect);
  onIntersectRef.current = onIntersect;

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!enabled || !sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          // One request per enabled cycle: the caller disables the trigger
          // while the requested page is in flight.
          observer.disconnect();
          onIntersectRef.current();
        }
      },
      { rootMargin },
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [enabled, rootMargin]);

  return sentinelRef;
}
