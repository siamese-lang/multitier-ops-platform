locals {
  name_prefix = "${var.project_name}-${var.environment}"

  selected_availability_zone = coalesce(
    var.availability_zone,
    data.aws_availability_zones.available.names[0]
  )

  app_nodes = merge(
    {
      "app-01" = {
        name = "app-01"
      }
    },
    var.enable_app_02 ? {
      "app-02" = {
        name = "app-02"
      }
    } : {}
  )

  validation_profile = {
    nat_gateway     = var.enable_nat_gateway
    app_02          = var.enable_app_02
    storage_node    = var.enable_storage_node
    backup_node     = var.enable_backup_node
    monitoring_node = var.enable_monitoring_node
    logging_node    = var.enable_logging_node
    loadgen_node    = var.enable_loadgen_node
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
