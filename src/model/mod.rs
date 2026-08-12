pub mod misc;
pub mod user;

#[macro_export]
macro_rules! impl_str_enum {
    ($enum_name:ident { $($variant:ident => $str:expr),* $(,)? }) => {
        impl std::fmt::Display for $enum_name {
            fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
                write!(f, "{}", self.as_str())
            }
        }

        impl std::str::FromStr for $enum_name {
            type Err = String;

            fn from_str(s: &str) -> Result<Self, Self::Err> {
                let candidate = s.split('/').next().unwrap_or(s);
                $(
                    if candidate == $str || candidate.eq_ignore_ascii_case($str) {
                        return Ok($enum_name::$variant);
                    }
                )*
                Err(format!("Unknown {}: {}", stringify!($enum_name), s))
            }
        }

        impl $enum_name {
            pub fn as_str(&self) -> &str {
                match self {
                    $($enum_name::$variant => $str,)*
                }
            }
        }
    };
}
