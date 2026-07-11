locals {
  name_prefix = "${var.project_name}-${var.environment}"

  selected_availability_zone = coalesce(
    var.availability_zone,
    data.aws_availability_zones.available.names[0]
  )

  common_tags = merge(
    {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Scope       = "lab-runtime"
      Repository  = "siamese-lang/multitier-ops-platform"
      RuntimePath = "nat-enabled"
    },
    var.extra_tags
  )
}
