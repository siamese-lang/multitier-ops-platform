locals {
  name_prefix = "${var.project_name}-${var.environment}"

  selected_availability_zone = coalesce(
    var.availability_zone,
    data.aws_availability_zones.available.names[0]
  )

  app_nodes = {
    "app-01" = {
      name = "app-01"
    }
    "app-02" = {
      name = "app-02"
    }
  }

  common_tags = merge(
    {
      Project      = var.project_name
      Environment  = var.environment
      ManagedBy    = "terraform"
      Scope        = "lab-full-ops"
      Repository   = "siamese-lang/multitier-ops-platform"
      Architecture = "web-was-db-storage-ops-backup"
    },
    var.extra_tags
  )
}
