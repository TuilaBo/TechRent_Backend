CREATE TABLE IF NOT EXISTS reservation (
    id SERIAL PRIMARY KEY,
    device_model_id INT NOT NULL REFERENCES device_model(device_model_id),
    order_detail_id INT NOT NULL REFERENCES order_detail(order_detail_id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reserved_quantity INT NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN (
        'PENDING_REVIEW',
        'UNDER_REVIEW',
        'CONFIRMED',
        'EXPIRED',
        'CANCELLED'
    )),
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL,
    expiration_time TIMESTAMP NULL
);
