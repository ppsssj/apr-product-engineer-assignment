"use client";

import { useRef } from "react";

// Allows at most one in-flight mutation, even for two triggers in the same
// tick: TanStack `isPending` only renders one scheduler tick after `mutate`.
export function useSingleFlight() {
  const inFlight = useRef(false);
  return {
    begin: () => {
      if (inFlight.current) return false;
      inFlight.current = true;
      return true;
    },
    end: () => {
      inFlight.current = false;
    },
  };
}
