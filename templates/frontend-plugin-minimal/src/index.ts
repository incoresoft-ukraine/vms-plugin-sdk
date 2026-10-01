// Async boundary required by Module Federation: shared modules must be negotiated before
// any synchronous import of vue etc.
import('./bootloader')
