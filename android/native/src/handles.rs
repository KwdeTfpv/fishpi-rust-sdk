use std::collections::HashMap;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Arc, Mutex};

pub(crate) struct HandleRegistry<T> {
    next: AtomicI64,
    entries: Mutex<HashMap<i64, Arc<T>>>,
}

impl<T: Send + Sync> HandleRegistry<T> {
    pub(crate) fn new() -> Self {
        Self {
            next: AtomicI64::new(1),
            entries: Mutex::new(HashMap::new()),
        }
    }

    pub(crate) fn insert(&self, value: T) -> i64 {
        let mut id = self.next.fetch_add(1, Ordering::Relaxed);
        if id == 0 {
            id = self.next.fetch_add(1, Ordering::Relaxed);
        }
        self.entries
            .lock()
            .unwrap_or_else(|error| error.into_inner())
            .insert(id, Arc::new(value));
        id
    }

    pub(crate) fn get(&self, id: i64) -> Option<Arc<T>> {
        if id == 0 {
            return None;
        }
        self.entries
            .lock()
            .unwrap_or_else(|error| error.into_inner())
            .get(&id)
            .cloned()
    }

    pub(crate) fn remove(&self, id: i64) -> Option<Arc<T>> {
        if id == 0 {
            return None;
        }
        self.entries
            .lock()
            .unwrap_or_else(|error| error.into_inner())
            .remove(&id)
    }
}
