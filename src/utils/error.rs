use std::error::Error as StdError;
use thiserror::Error;

#[derive(Error, Debug)]
pub enum Error {
    #[error("Request error: {0}")]
    Request(Box<dyn StdError + Send + Sync>),
    #[error("{0}")]
    Api(String),
    #[error("Parse error: {0}")]
    Parse(String),
}
